package com.wavii.service;

import com.wavii.dto.forum.CreateForumRequest;
import com.wavii.dto.forum.CreatePostRequest;
import com.wavii.dto.forum.ForumMemberResponse;
import com.wavii.dto.forum.ForumResponse;
import com.wavii.dto.forum.ForumSummaryResponse;
import com.wavii.dto.forum.PostResponse;
import com.wavii.dto.forum.UpdateForumRequest;
import com.wavii.model.Forum;
import com.wavii.model.ForumLike;
import com.wavii.model.ForumMembership;
import com.wavii.model.ForumPost;
import com.wavii.model.User;
import com.wavii.model.enums.ForumCategory;
import com.wavii.model.enums.ForumMembershipRole;
import com.wavii.model.enums.ForumSortOption;
import com.wavii.repository.ForumLikeRepository;
import com.wavii.repository.ForumMembershipRepository;
import com.wavii.repository.ForumPostRepository;
import com.wavii.repository.ForumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de foros y comunidades en Wavii.
 * Proporciona métodos para crear foros, gestionar membresías, publicaciones e interacciones.
 * 
 * @author danielrguezh
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForumService {

    private final ForumRepository forumRepository;
    private final ForumMembershipRepository membershipRepository;
    private final ForumPostRepository postRepository;
    private final ForumLikeRepository likeRepository;

    /**
     * Obtiene la lista de foros filtrados por búsqueda, ciudad y categoría.
     * 
     * @param search Texto a buscar en el nombre.
     * @param city Ciudad para filtrar.
     * @param category Categoría del foro.
     * @param sort Criterio de ordenación.
     * @param currentUser Usuario actual para marcar estados de pertenencia y likes.
     * @return Lista de resúmenes de foros.
     */
    @Transactional(readOnly = true)
    public List<ForumSummaryResponse> getForums(String search, String city, String category, String sort, User currentUser) {
        String normalizedSearch = search != null && !search.isBlank() ? search.trim().toLowerCase() : null;
        String normalizedCity = city != null && !city.isBlank() ? city.trim().toLowerCase() : null;
        ForumCategory categoryFilter = parseEnum(ForumCategory.class, category);
        ForumSortOption sortOption = parseEnum(ForumSortOption.class, sort);
        Sort sortOrder = switch (sortOption != null ? sortOption : ForumSortOption.MOST_LIKED) {
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case LEAST_LIKED -> Sort.by(Sort.Direction.ASC, "likeCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case MOST_LIKED -> Sort.by(Sort.Direction.DESC, "likeCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        };

        Specification<Forum> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedSearch != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + normalizedSearch + "%"));
            }
            if (normalizedCity != null) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + normalizedCity + "%"));
            }
            if (categoryFilter != null) {
                predicates.add(cb.equal(root.get("category"), categoryFilter));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Forum> forums = forumRepository
                .findAll(spec, PageRequest.of(0, 50, sortOrder))
                .getContent();

        Set<UUID> joinedIds = Collections.emptySet();
        Set<UUID> likedIds = Collections.emptySet();
        if (currentUser != null && !forums.isEmpty()) {
            List<UUID> forumIds = forums.stream().map(Forum::getId).collect(Collectors.toList());
            joinedIds = new HashSet<>(membershipRepository.findJoinedForumIds(currentUser, forumIds));
            likedIds = new HashSet<>(likeRepository.findLikedForumIds(currentUser, forumIds));
        }

        final Set<UUID> finalJoinedIds = joinedIds;
        final Set<UUID> finalLikedIds = likedIds;
        return forums.stream()
                .map((forum) -> toSummary(forum, finalJoinedIds.contains(forum.getId()), finalLikedIds.contains(forum.getId())))
                .toList();
    }

    /**
     * Obtiene los detalles de un foro específico por su ID.
     * 
     * @param id ID del foro.
     * @param currentUser Usuario actual.
     * @return Respuesta con los detalles del foro.
     */
    @Transactional(readOnly = true)
    public ForumResponse getForumById(UUID id, User currentUser) {
        Forum forum = forumRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foro no encontrado"));
        boolean joined = currentUser != null && membershipRepository.existsByForumAndUser(forum, currentUser);
        boolean liked = currentUser != null && likeRepository.existsByForumAndUser(forum, currentUser);
        return toResponse(forum, joined, liked, currentUser);
    }

    /**
     * Crea un nuevo foro en la plataforma.
     * 
     * @param request Datos de creación.
     * @param creator Usuario creador.
     * @return Respuesta con el foro creado.
     */
    @Transactional
    public ForumResponse createForum(CreateForumRequest request, User creator) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (request.category() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoria es obligatoria");
        }

        Forum forum = Forum.builder()
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .category(request.category())
                .coverImageUrl(request.coverImageUrl())
                .city(request.city() != null && !request.city().isBlank() ? request.city().trim() : null)
                .creator(creator)
                .memberCount(1)
                .build();
        forum = forumRepository.save(forum);

        membershipRepository.save(ForumMembership.builder()
                .forum(forum)
                .user(creator)
                .role(ForumMembershipRole.OWNER)
                .build());

        return toResponse(forum, true, false, creator);
    }

    @Transactional
    public ForumResponse updateForum(UUID forumId, UpdateForumRequest request, User currentUser) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foro no encontrado"));

        ForumMembership membership = membershipRepository.findByForumAndUser(forum, currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este foro"));

        if (membership.getRole() != ForumMembershipRole.OWNER && membership.getRole() != ForumMembershipRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los administradores pueden editar el grupo");
        }

        if (request.description() != null) {
            forum.setDescription(request.description().trim().isEmpty() ? null : request.description().trim());
        }
        if (request.coverImageUrl() != null) {
            forum.setCoverImageUrl(request.coverImageUrl());
        }
        if (request.category() != null) {
            forum.setCategory(request.category());
        }

        forum = forumRepository.save(forum);
        boolean liked = likeRepository.existsByForumAndUser(forum, currentUser);
        return toResponse(forum, true, liked, currentUser);
    }

    /**
     * Une al usuario actual a un foro.
     *
     * @param forumId ID del foro.
     * @param user Usuario actual.
     */
    @Transactional
    public void joinForum(UUID forumId, User user) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foro no encontrado"));

        if (membershipRepository.existsByForumAndUser(forum, user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya eres miembro de este foro");
        }

        membershipRepository.save(ForumMembership.builder()
                .forum(forum)
                .user(user)
                .build());
        forumRepository.incrementMemberCount(forumId);
    }

    /**
     * Hace que el usuario abandone un foro.
     * 
     * @param forumId ID del foro.
     * @param user Usuario actual.
     */
    @Transactional
    public void leaveForum(UUID forumId, User user) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foro no encontrado"));

        ForumMembership membership = membershipRepository.findByForumAndUser(forum, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No eres miembro de este foro"));

        membershipRepository.delete(membership);
        reconcileAfterMemberRemoval(forum);
    }

    /**
     * Obtiene la lista de foros a los que pertenece el usuario.
     * 
     * @param user Usuario actual.
     * @return Lista de resúmenes de sus foros.
     */
    @Transactional(readOnly = true)
    public List<ForumSummaryResponse> getMyForums(User user) {
        return membershipRepository.findByUserWithForum(user).stream()
                .map((membership) -> toSummary(membership.getForum(), true,
                        likeRepository.existsByForumAndUser(membership.getForum(), user)))
                .toList();
    }

    /**
     * Lista los miembros de un foro.
     * 
     * @param forumId ID del foro.
     * @param requester Usuario que realiza la consulta (debe ser miembro).
     * @return Lista de miembros con sus roles.
     */
    @Transactional(readOnly = true)
    public List<ForumMemberResponse> getMembers(UUID forumId, User requester) {
        Forum forum = findForum(forumId);
        ensureMember(forum, requester);
        return getMemberResponses(forum);
    }

    /**
     * Registra un "me gusta" en un foro.
     * 
     * @param forumId ID del foro.
     * @param user Usuario actual.
     * @return Respuesta actualizada del foro.
     */
    @Transactional
    public ForumResponse likeForum(UUID forumId, User user) {
        Forum forum = findForum(forumId);
        if (!likeRepository.existsByForumAndUser(forum, user)) {
            likeRepository.save(ForumLike.builder().forum(forum).user(user).build());
            forumRepository.incrementLikeCount(forumId);
            forum.setLikeCount(forum.getLikeCount() + 1);
        }
        boolean joined = membershipRepository.existsByForumAndUser(forum, user);
        return toResponse(forum, joined, true, user);
    }

    /**
     * Retira un "me gusta" de un foro.
     * 
     * @param forumId ID del foro.
     * @param user Usuario actual.
     * @return Respuesta actualizada del foro.
     */
    @Transactional
    public ForumResponse unlikeForum(UUID forumId, User user) {
        Forum forum = findForum(forumId);
        likeRepository.findByForumAndUser(forum, user).ifPresent(like -> {
            likeRepository.delete(like);
            forumRepository.decrementLikeCount(forumId);
            forum.setLikeCount(Math.max(0, forum.getLikeCount() - 1));
        });
        boolean joined = membershipRepository.existsByForumAndUser(forum, user);
        return toResponse(forum, joined, false, user);
    }

    /**
     * Actualiza el rol de un miembro del foro (solo permitido para el OWNER).
     * 
     * @param forumId ID del foro.
     * @param userId ID del miembro a actualizar.
     * @param role Nuevo rol.
     * @param requester Usuario que realiza la acción.
     * @return Respuesta actualizada del foro.
     */
    @Transactional
    public ForumResponse updateMemberRole(UUID forumId, UUID userId, ForumMembershipRole role, User requester) {
        Forum forum = findForum(forumId);
        ForumMembership requesterMembership = ensureMember(forum, requester);
        if (requesterMembership.getRole() != ForumMembershipRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el creador puede cambiar administradores");
        }
        if (role == null || role == ForumMembershipRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no valido");
        }
        ForumMembership target = membershipRepository.findByForumOrderByJoinedAtAsc(forum).stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado"));
        if (target.getRole() == ForumMembershipRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes cambiar el rol del creador");
        }
        target.setRole(role);
        membershipRepository.save(target);
        return toResponse(forum, true, likeRepository.existsByForumAndUser(forum, requester), requester);
    }

    /**
     * Expulsa a un miembro del foro (solo permitido para ADMIN u OWNER).
     * 
     * @param forumId ID del foro.
     * @param userId ID del miembro a expulsar.
     * @param requester Usuario que realiza la acción.
     */
    @Transactional
    public void removeMember(UUID forumId, UUID userId, User requester) {
        Forum forum = findForum(forumId);
        ForumMembership requesterMembership = ensureMember(forum, requester);
        if (requesterMembership.getRole() == ForumMembershipRole.MEMBER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo admins pueden expulsar miembros");
        }
        ForumMembership target = membershipRepository.findByForumOrderByJoinedAtAsc(forum).stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado"));
        if (target.getRole() == ForumMembershipRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes expulsar al creador");
        }
        membershipRepository.delete(target);
        reconcileAfterMemberRemoval(forum);
    }

    /**
     * Obtiene los mensajes (posts) de un foro en formato paginado.
     * 
     * @param forumId ID del foro.
     * @param page Número de página.
     * @param currentUser Usuario actual.
     * @return Página de mensajes.
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(UUID forumId, int page, User currentUser) {
        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foro no encontrado"));

        if (!membershipRepository.existsByForumAndUser(forum, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes unirte al foro para ver los mensajes");
        }

        return postRepository.findByForumOrderByCreatedAtDesc(forum, PageRequest.of(page, 20))
                .map(this::toPostResponse);
    }

    @Transactional
    public PostResponse createPost(UUID forumId, CreatePostRequest request, User author) {
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje no puede estar vacio");
        }

        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foro no encontrado"));

        if (!membershipRepository.existsByForumAndUser(forum, author)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes unirte al foro para escribir mensajes");
        }

        ForumPost post = ForumPost.builder()
                .forum(forum)
                .author(author)
                .content(request.content().trim())
                .build();

        return toPostResponse(postRepository.save(post));
    }

    private ForumSummaryResponse toSummary(Forum forum, boolean joined, boolean likedByMe) {
        return new ForumSummaryResponse(
                forum.getId().toString(),
                forum.getName(),
                forum.getDescription(),
                forum.getCategory().name(),
                forum.getMemberCount(),
                joined,
                forum.getCoverImageUrl(),
                forum.getCreator().getName(),
                forum.getCity(),
                forum.getLikeCount(),
                likedByMe
        );
    }

    private ForumResponse toResponse(Forum forum, boolean joined, boolean likedByMe, User currentUser) {
        String role = currentUser == null ? null : membershipRepository.findByForumAndUser(forum, currentUser)
                .map(membership -> membership.getRole().name())
                .orElse(null);
        return new ForumResponse(
                forum.getId().toString(),
                forum.getName(),
                forum.getDescription(),
                forum.getCategory().name(),
                forum.getMemberCount(),
                joined,
                forum.getCoverImageUrl(),
                forum.getCreator().getId().toString(),
                forum.getCreator().getName(),
                forum.getCity(),
                forum.getCreatedAt().toString(),
                forum.getLikeCount(),
                likedByMe,
                role,
                getMemberResponses(forum)
        );
    }

    private PostResponse toPostResponse(ForumPost post) {
        return new PostResponse(
                post.getId().toString(),
                post.getContent(),
                post.getAuthor().getId().toString(),
                post.getAuthor().getName(),
                post.getAuthor().getAvatarUrl(),
                post.getCreatedAt().toString()
        );
    }

    private Forum findForum(UUID forumId) {
        return forumRepository.findById(forumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foro no encontrado"));
    }

    private ForumMembership ensureMember(Forum forum, User user) {
        return membershipRepository.findByForumAndUser(forum, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes unirte al foro"));
    }

    private List<ForumMemberResponse> getMemberResponses(Forum forum) {
        return membershipRepository.findByForumOrderByJoinedAtAsc(forum).stream()
                .map((membership) -> new ForumMemberResponse(
                        membership.getUser().getId().toString(),
                        membership.getUser().getName(),
                        membership.getUser().getAvatarUrl(),
                        membership.getRole().name(),
                        membership.getJoinedAt().toString()
                ))
                .toList();
    }

    private void reconcileAfterMemberRemoval(Forum forum) {
        List<ForumMembership> remaining = membershipRepository.findByForumOrderByJoinedAtAsc(forum);
        if (remaining.isEmpty()) {
            deleteForum(forum);
            return;
        }

        forum.setMemberCount(remaining.size());
        boolean hasOwner = remaining.stream().anyMatch(membership -> membership.getRole() == ForumMembershipRole.OWNER);
        if (!hasOwner) {
            ForumMembership nextOwner = remaining.stream()
                    .filter(membership -> membership.getRole() == ForumMembershipRole.ADMIN)
                    .findFirst()
                    .orElse(remaining.get(0));
            nextOwner.setRole(ForumMembershipRole.OWNER);
            forum.setCreator(nextOwner.getUser());
            membershipRepository.save(nextOwner);
        }
        forumRepository.save(forum);
    }

    private void deleteForum(Forum forum) {
        postRepository.deleteByForum(forum);
        likeRepository.deleteByForum(forum);
        membershipRepository.deleteByForum(forum);
        forumRepository.delete(forum);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(cls, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
