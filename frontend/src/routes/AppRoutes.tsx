import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { ProtectedRoute } from './ProtectedRoute';
import { DashboardLayout } from '../components/layout/DashboardLayout';
import { EmployeeDashboardPage } from '../pages/EmployeeDashboardPage';
import { KnowledgePage } from '../pages/KnowledgePage';
import { MeetingsPage } from '../pages/MeetingsPage';
import { AiUtilitiesPage } from '../pages/AiUtilitiesPage';
import { ChatPage } from '../pages/ChatPage';
import { AdminDashboardPage } from '../pages/AdminDashboardPage';
import { UserManagementPage } from '../pages/UserManagementPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Authentication Routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Protected Enterprise Routes */}
      <Route element={<ProtectedRoute />}>
        <Route path="/app" element={<DashboardLayout />}>
          <Route path="dashboard" element={<EmployeeDashboardPage />} />
          <Route path="chat" element={<ChatPage />} />
          <Route path="knowledge" element={<KnowledgePage />} />
          <Route path="meetings" element={<MeetingsPage />} />
          <Route path="utilities" element={<AiUtilitiesPage />} />

          {/* Admin Restricted Routes */}
          <Route element={<ProtectedRoute requiredRole="ROLE_ADMIN" />}>
            <Route path="admin/dashboard" element={<AdminDashboardPage />} />
            <Route path="admin/users" element={<UserManagementPage />} />
            <Route path="admin/audit-logs" element={<div className="p-4 text-slate-400">Security Audit Logs (Milestone 5)</div>} />
          </Route>
        </Route>
      </Route>

      {/* Fallback Redirect */}
      <Route path="*" element={<Navigate to="/app/dashboard" replace />} />
    </Routes>
  );
};
