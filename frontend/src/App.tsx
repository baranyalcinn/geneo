import './App.css';
import { RouterProvider } from 'react-router-dom';
import AppProviders from './context/AppProviders';
import { router } from './routes';
import { Toaster } from 'react-hot-toast';

function App() {
  return (
    <AppProviders>
      <RouterProvider router={router} />
      <Toaster 
        position="top-right"
        reverseOrder={false}
        gutter={8}
        toastOptions={{
          duration: 4000,
          style: {
            background: '#363636',
            color: '#fff',
            fontSize: '14px',
            borderRadius: '8px',
            padding: '12px 16px',
          },
          success: {
            duration: 3000,
            iconTheme: {
              primary: '#10B981',
              secondary: '#fff',
            },
          },
          error: {
            duration: 4000,
            iconTheme: {
              primary: '#EF4444',
              secondary: '#fff',
            },
          },
          loading: {
            duration: Infinity,
          },
        }}
      />
    </AppProviders>
  );
}

export default App;
