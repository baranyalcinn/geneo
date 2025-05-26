import React, { Suspense } from 'react';
import { Outlet } from 'react-router-dom';
import { CircularProgress } from '@mui/material';
import Navbar from '../components/navbar/Navbar';
import '../App.css'; // Stil dosyasını import ediyoruz

// Yükleme bileşeni
const LoadingFallback = () => (
  <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 'calc(100vh - 64px)' }}> {/* Navbar yüksekliğini hesaba katalım (varsayım) */}
    <CircularProgress />
  </div>
);

export const AppLayout: React.FC = () => {
  return (
    <>
      <Navbar />
      {/* Stil dosyasından gelen .content sınıfını kullanıyoruz */}
      <div className="content">
        <Suspense fallback={<LoadingFallback />}>
          <Outlet />
        </Suspense>
      </div>
    </>
  );
}; 