import React, { useState } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { api } from '../../services/api';
import { landingPathFor } from '../../components/layout/RoleRoute';

interface LoginResponse {
  accessToken: string;
  expiresIn: number;
  user: {
    id: string;
    username: string;
    role: string;
    marketId: string;
    farmerId: string | null;
    buyerId: string | null;
  };
}

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const { login, user } = useAuth();
  const navigate = useNavigate();

  if (user) {
    // A farmer or buyer must not be dropped onto the agent dashboard.
    return <Navigate to={landingPathFor(user.role)} replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      const response = await api.post<LoginResponse>('/api/auth/login', { username, password });
      login(response.accessToken, response.user);
      navigate(landingPathFor(response.user.role));
    } catch (err: any) {
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-surface-container-lowest flex items-center justify-center p-space-md">
      <div className="max-w-md w-full bg-background rounded-xl shadow-sm p-space-xl border border-surface-container-high">
        <div className="flex flex-col items-center mb-space-lg">
          <div className="flex items-center gap-space-xs mb-space-sm">
            <span className="text-headline-xl text-primary tracking-tight truncate">AgriMandi AI</span>
            <span className="px-space-xs py-space-xxs bg-primary-container text-on-primary text-label-sm rounded uppercase">APMC</span>
          </div>
          <h1 className="text-headline-lg text-on-surface">Sign In</h1>
          <p className="text-body-sm text-on-surface-variant text-center mt-space-xs">
            Operational Suite v4.2 • Authorized APMC Personnel Only
          </p>
        </div>

        {error && (
          <div className="mb-space-md p-space-sm bg-error-container text-on-error-container text-label-sm rounded">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-space-md">
          <div className="flex flex-col gap-space-xs">
            <label className="text-label-lg text-on-surface-variant" htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="px-space-md py-space-sm bg-surface-container-low text-on-surface placeholder:text-on-surface-variant text-body-md rounded-lg focus:outline-none focus:bg-surface-container border border-transparent focus:border-primary-fixed-dim"
              required
            />
          </div>
          <div className="flex flex-col gap-space-xs">
            <label className="text-label-lg text-on-surface-variant" htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="px-space-md py-space-sm bg-surface-container-low text-on-surface placeholder:text-on-surface-variant text-body-md rounded-lg focus:outline-none focus:bg-surface-container border border-transparent focus:border-primary-fixed-dim"
              required
            />
          </div>
          <button
            type="submit"
            disabled={isLoading}
            className="mt-space-sm w-full py-space-sm bg-primary text-on-primary text-label-lg rounded-lg hover:bg-primary-container hover:text-on-primary-container transition-colors shadow-sm disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {isLoading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  );
}
