import React, { Suspense, lazy } from 'react';
import { createBrowserRouter } from 'react-router-dom';
import { AppLayout } from './layouts/AppLayout'; // Yeni Layout import edildi

// Lazy loading ile sayfa bileşenleri
const AddPerson = lazy(() => import('./pages/AddPerson')); // Yeni kişi ekleme için kullanılacak
const FamilyRelationGame = lazy(() => import('./pages/FamilyRelationGame'));
const PersonListPage = lazy(() => import('./pages/PersonListPage'));
// PersonPage -> PersonDetailPage olarak düşünüyoruz
const PersonDetailPage = lazy(() => import('./pages/PersonDetailPage')); // Dosya adının da değiştiğini varsayıyoruz
const FamilyTreePage = lazy(() => import('./pages/FamilyTreePage'));

// Yükleme bileşeni ve Layout tanımı kaldırıldı, AppLayout'tan geliyor

// React Router yapılandırması
export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />, // AppLayout kullanılıyor
    children: [
      {
        index: true, // Anasayfa için daha iyi bir pratik
        element: <PersonListPage />,
      },
      // { // /add-person rotası kaldırıldı
      //   path: "add-person",
      //   element: <AddPerson />,
      // },
      {
        path: "game",
        element: <FamilyRelationGame />,
      },
      {
        path: "persons",
        children: [
            {
                index: true,
                element: <PersonListPage />,
            },
            {
                path: "new", // Artık AddPerson bileşenini kullanıyor
                element: <AddPerson />, 
            },
            {
                path: ":id", // Artık PersonDetailPage bileşenini kullanıyor
                element: <PersonDetailPage />,
            },
        ]
      },
      {
        path: "family-tree",
        element: <FamilyTreePage />,
      }
    ]
  }
]); 