import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true, // Assuming Cookie-based auth
  headers: {
    'Content-Type': 'application/json',
  },
  paramsSerializer: (params) => {
    const searchParams = new URLSearchParams();
    for (const key in params) {
      const value = params[key];
      if (value === undefined || value === null) continue;
      
      if (Array.isArray(value)) {
        value.forEach(v => searchParams.append(key, v));
      } else {
        searchParams.append(key, value);
      }
    }
    return searchParams.toString();
  }
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    return Promise.reject(error);
  }
);
