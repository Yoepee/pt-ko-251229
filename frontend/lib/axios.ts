import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true, // Assuming Cookie-based auth
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    return Promise.reject(error);
  }
);
