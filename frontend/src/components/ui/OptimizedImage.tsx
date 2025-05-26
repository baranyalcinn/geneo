import React, { useState } from 'react';

interface OptimizedImageProps {
  src: string;
  alt: string;
  width?: number;
  height?: number;
  className?: string;
  placeholder?: string;
}

const OptimizedImage: React.FC<OptimizedImageProps> = ({
  src,
  alt,
  width,
  height,
  className = '',
  placeholder = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2VlZWVlZSIvPjwvc3ZnPg=='
}) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [error, setError] = useState(false);
  
  // Gerçek bir CDN kullanılacaksa burası değiştirilmeli
  // Şimdilik doğrudan kaynak kullanılıyor 
  const imgSrc = src;
  
  // Width ve height değerleri varsa sorgu parametreleri ekleyin
  // Not: Backend'in bu parametreleri desteklemesi gerekir
  const optimizedSrc = imgSrc;
  
  return (
    <div className={`optimized-image-container ${className}`}>
      <img
        src={error ? placeholder : optimizedSrc}
        alt={alt}
        width={width}
        height={height}
        style={{
          opacity: isLoaded ? 1 : 0,
          transition: 'opacity 0.3s ease-in-out',
          objectFit: 'cover'
        }}
        onLoad={() => setIsLoaded(true)}
        onError={() => {
          setError(true);
          setIsLoaded(true);
        }}
        loading="lazy"
      />
      {!isLoaded && (
        <div 
          className="image-placeholder" 
          style={{
            backgroundImage: `url(${placeholder})`,
            width: width || '100%',
            height: height || '100%'
          }}
        />
      )}
    </div>
  );
};

export default OptimizedImage; 