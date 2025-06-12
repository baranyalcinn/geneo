import '@testing-library/jest-dom'
import { vi } from 'vitest'

// Global test setup
global.ResizeObserver = global.ResizeObserver || 
  class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  }

// Mock IntersectionObserver
global.IntersectionObserver = global.IntersectionObserver ||
  class IntersectionObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  }

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // deprecated
    removeListener: vi.fn(), // deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
}) 