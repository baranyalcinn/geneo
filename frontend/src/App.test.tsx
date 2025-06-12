import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import App from './App';

// Mock the API requests
beforeEach(() => {
  global.fetch = vi.fn(() =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve([]),
    })
  ) as any;
});

describe('App component', () => {
  it('renders family tree app title', async () => {
    render(<App />);
    
    // Wait for the component to load and check for the app title
    await waitFor(() => {
      expect(screen.getByText('Aile Ağacı')).toBeInTheDocument();
    });
  });

  it('renders navigation links', async () => {
    render(<App />);
    
    await waitFor(() => {
      expect(screen.getByText('Home')).toBeInTheDocument();
      expect(screen.getByText('persons')).toBeInTheDocument();
      expect(screen.getByText('familyTree')).toBeInTheDocument();
      expect(screen.getByText('relationGame')).toBeInTheDocument();
    });
  });
});
