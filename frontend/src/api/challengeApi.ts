import { http } from './http';
import { PUBLIC_BASE_URL } from './config';

export type ChallengeLevel = 'PRINCIPIANTE' | 'INTERMEDIO' | 'AVANZADO';

export interface DailyChallengeDto {
  id: number;
  challengeDate: string;
  difficulty: ChallengeLevel;
  xpReward: number;
  tabId: number;
  tabTitle: string | null;
  tabOwnerName: string | null;
  tabDescription: string | null;
  tabCoverImageUrl: string | null;
  completedByMe: boolean;
}

export interface CompleteChallengResponse {
  xpGained: number;
  totalXp: number;
  newLevel: number;
  leveledUp: boolean;
  streak: number;
  bestStreak: number;
}

export interface StatsDto {
  streak: number;
  bestStreak: number;
  xp: number;
  level: number;
  completedDaysThisMonth: string[]; // ISO dates: "2026-05-01"
  completedThisWeek: number;
}

export async function apiFetchTodayChallenges(): Promise<DailyChallengeDto[]> {
  const { data } = await http.get<DailyChallengeDto[]>('/api/challenges/today');
  return data.map((challenge) => ({
    ...challenge,
    tabCoverImageUrl: challenge.tabCoverImageUrl
      ? challenge.tabCoverImageUrl.startsWith('http')
        ? challenge.tabCoverImageUrl
        : `${PUBLIC_BASE_URL}${challenge.tabCoverImageUrl}`
      : null,
  }));
}

export async function apiCompleteChallenge(
  challengeId: number,
): Promise<CompleteChallengResponse> {
  const { data } = await http.post<CompleteChallengResponse>(
    `/api/challenges/${challengeId}/complete`,
    null,
  );
  return data;
}

export async function apiFetchStats(): Promise<StatsDto> {
  const { data } = await http.get<StatsDto>('/api/challenges/stats');
  return data;
}
