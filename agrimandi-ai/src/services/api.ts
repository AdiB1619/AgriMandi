export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export class ApiError extends Error {
  status: number;
  data: any;

  constructor(status: number, message: string, data?: any) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

async function handleResponse(response: Response) {
  if (!response.ok) {
    let errorMessage = response.statusText;
    let data;
    try {
      data = await response.json();
      errorMessage = data.message || errorMessage;
    } catch {
      // Not JSON
    }
    
    if (response.status === 401) {
      // Clear token and reload to trigger login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }

    throw new ApiError(response.status, errorMessage, data);
  }

  // Handle empty responses
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  getHeaders() {
    const token = localStorage.getItem('token');
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  },

  async get<T>(url: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'GET',
      headers: this.getHeaders(),
    });
    return handleResponse(response);
  },

  async post<T>(url: string, body?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'POST',
      headers: this.getHeaders(),
      body: body ? JSON.stringify(body) : undefined,
    });
    return handleResponse(response);
  },

  async put<T>(url: string, body?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'PUT',
      headers: this.getHeaders(),
      body: body ? JSON.stringify(body) : undefined,
    });
    return handleResponse(response);
  },

  async delete<T>(url: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'DELETE',
      headers: this.getHeaders(),
    });
    return handleResponse(response);
  }
};
