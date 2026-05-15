import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { NavigatorScreenParams } from '@react-navigation/native';
import { MainNavigator } from './MainNavigator';
import { BulletinBoardScreen } from '../screens/bulletin/BulletinBoardScreen';
import { ProfileScreen } from '../screens/profile/ProfileScreen';
import { ChangePasswordScreen } from '../screens/profile/ChangePasswordScreen';
import { SubscriptionScreen } from '../screens/profile/SubscriptionScreen';
import { SubscriptionPlanScreen } from '../screens/profile/SubscriptionPlanScreen';
import { HelpScreen } from '../screens/profile/HelpScreen';
import { TermsScreen } from '../screens/profile/TermsScreen';
import { TeacherVerificationScreen } from '../screens/profile/TeacherVerificationScreen';
import { GroupsScreen } from '../screens/groups/GroupsScreen';
import { ForumDetailScreen } from '../screens/groups/ForumDetailScreen';
import { CreateForumScreen } from '../screens/groups/CreateForumScreen';
import { MyForumsScreen } from '../screens/groups/MyForumsScreen';
import { ForumProfileScreen } from '../screens/groups/ForumProfileScreen';
import { BandsScreen } from '../screens/bands/BandsScreen';
import { BandDetailScreen } from '../screens/bands/BandDetailScreen';
import { CreateBandListingScreen } from '../screens/bands/CreateBandListingScreen';
import { UploadTabScreen } from '../screens/tabs/UploadTabScreen';
import { PdfViewerScreen } from '../screens/tabs/PdfViewerScreen';
import { StatsScreen } from '../screens/stats/StatsScreen';
import { NewsScreen } from '../screens/news/NewsScreen';
import { ArticleScreen } from '../screens/news/ArticleScreen';
import { ChallengesScreen } from '../screens/challenges/ChallengesScreen';
import { TeacherProfileScreen } from '../screens/bulletin/TeacherProfileScreen';
import { ClassesScreen } from '../screens/classes/ClassesScreen';
import { ClassPostsScreen } from '../screens/classes/ClassPostsScreen';
import { ClassRoomScreen } from '../screens/classes/ClassRoomScreen';
import { ManageClassesScreen } from '../screens/classes/ManageClassesScreen';
import { TeacherClassStudentsScreen } from '../screens/classes/TeacherClassStudentsScreen';
import { UserProfileScreen } from '../screens/profile/UserProfileScreen';
import { UserTabsScreen } from '../screens/profile/UserTabsScreen';
import { DirectMessageScreen } from '../screens/messages/DirectMessageScreen';
import type { MainTabParamList } from './MainNavigator';

export type AppStackParamList = {
  MainTabs: NavigatorScreenParams<MainTabParamList> | undefined;
  BulletinBoard: undefined;
  Profile: undefined;
  ChangePassword: undefined;
  Subscription: { isOnboarding?: boolean } | undefined;
  SubscriptionPlan: { planId: 'free' | 'plus' | 'education' };
  TeacherVerification: undefined;
  Help: undefined;
  Terms: undefined;
  Social: undefined;
  ForumDetail: { forumId: string };
  ForumProfile: { forumId: string };
  MyForums: undefined;
  CreateForum: undefined;
  Bands: undefined;
  BandDetail: { listingId: string };
  CreateBandListing: undefined;
  UploadTab: undefined;
  PdfViewer: { pdfId: number; title: string };
  Challenges: undefined;
  News: undefined;
  Stats: undefined;
  Article: { url: string; title: string; sourceName?: string };
  TeacherProfile: { teacherId: string };
  Classes: undefined;
  ClassPosts: undefined;
  ClassRoom: { enrollmentId: string; teacherName: string; teacherId: string; studentId: string; studentName: string };
  TeacherClassStudents: { mode?: 'students' | 'requests' } | undefined;
  ManageClasses: { focusEnrollmentId?: string } | undefined;
  UserProfile: { userId: string };
  UserTabs: { userId: string; userName: string };
  DirectMessage: { userId: string; userName: string };
};

const Stack = createNativeStackNavigator<AppStackParamList>();

export const AppNavigator = () => (
  <Stack.Navigator screenOptions={{ headerShown: false }}>
    <Stack.Screen name="MainTabs" component={MainNavigator} />
    <Stack.Screen name="BulletinBoard" component={BulletinBoardScreen} />
    <Stack.Screen name="Profile" component={ProfileScreen} />
    <Stack.Screen name="ChangePassword" component={ChangePasswordScreen} />
    <Stack.Screen name="Subscription" component={SubscriptionScreen} />
    <Stack.Screen name="SubscriptionPlan" component={SubscriptionPlanScreen} />
    <Stack.Screen name="TeacherVerification" component={TeacherVerificationScreen} />
    <Stack.Screen name="Help" component={HelpScreen} />
    <Stack.Screen name="Terms" component={TermsScreen} />
    <Stack.Screen name="Social" component={GroupsScreen} />
    <Stack.Screen name="ForumDetail" component={ForumDetailScreen} />
    <Stack.Screen name="ForumProfile" component={ForumProfileScreen} />
    <Stack.Screen name="MyForums" component={MyForumsScreen} />
    <Stack.Screen name="CreateForum" component={CreateForumScreen} />
    <Stack.Screen name="Bands" component={BandsScreen} />
    <Stack.Screen name="BandDetail" component={BandDetailScreen} />
    <Stack.Screen name="CreateBandListing" component={CreateBandListingScreen} />
    <Stack.Screen name="UploadTab" component={UploadTabScreen} />
    <Stack.Screen name="PdfViewer" component={PdfViewerScreen} />
    <Stack.Screen name="Challenges" component={ChallengesScreen} />
    <Stack.Screen name="News" component={NewsScreen} />
    <Stack.Screen name="Stats" component={StatsScreen} />
    <Stack.Screen name="Article" component={ArticleScreen} />
    <Stack.Screen name="TeacherProfile" component={TeacherProfileScreen} />
    <Stack.Screen name="Classes" component={ClassesScreen} />
    <Stack.Screen name="ClassPosts" component={ClassPostsScreen} />
    <Stack.Screen name="ClassRoom" component={ClassRoomScreen} />
    <Stack.Screen name="TeacherClassStudents" component={TeacherClassStudentsScreen} />
    <Stack.Screen name="ManageClasses" component={ManageClassesScreen} />
    <Stack.Screen name="UserProfile" component={UserProfileScreen} />
    <Stack.Screen name="UserTabs" component={UserTabsScreen} />
    <Stack.Screen name="DirectMessage" component={DirectMessageScreen} />
  </Stack.Navigator>
);
