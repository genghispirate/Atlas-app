import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { Place } from '../lib/db';

interface MapViewProps {
  places: Place[];
  onPlaceSelect?: (place: Place) => void;
  selectedPlace?: Place | null;
}

export default function MapView({ places, onPlaceSelect, selectedPlace }: MapViewProps) {
  const mapRef = useRef<L.Map | null>(null);
  const markersRef = useRef<L.LayerGroup | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [tileError, setTileError] = useState(false);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    mapRef.current = L.map(containerRef.current, {
      center: [20, 0],
      zoom: 2,
      zoomControl: false,
      attributionControl: false,
    });

    const tileLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
    });

    tileLayer.on('tileerror', () => {
      setTileError(true);
    });

    tileLayer.addTo(mapRef.current);
    markersRef.current = L.layerGroup().addTo(mapRef.current);
    L.control.zoom({ position: 'bottomright' }).addTo(mapRef.current);

    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current || !markersRef.current) return;

    markersRef.current.clearLayers();

    places.forEach((place) => {
      const color = place.visited ? '#f59e0b' : '#3b82f6';
      const isSelected = selectedPlace?.id === place.id;

      const icon = L.divIcon({
        className: 'custom-marker',
        html: `<div style="width:${isSelected ? 16 : 10}px;height:${isSelected ? 16 : 10}px;background:${isSelected ? '#22d3ee' : color};border-radius:50%;border:2px solid #fff;box-shadow:0 0 ${isSelected ? 12 : 6}px ${isSelected ? '#22d3ee' : color}"></div>`,
        iconSize: [isSelected ? 16 : 10, isSelected ? 16 : 10],
        iconAnchor: [isSelected ? 8 : 5, isSelected ? 8 : 5],
      });

      const marker = L.marker([place.lat, place.lng], { icon })
        .addTo(markersRef.current!)
        .bindTooltip(`<b>${place.name}</b><br/>${place.country}`, {
          direction: 'top',
          offset: [0, -8],
        });

      marker.on('click', () => onPlaceSelect?.(place));
    });
  }, [places, selectedPlace, onPlaceSelect]);

  useEffect(() => {
    if (selectedPlace && mapRef.current) {
      mapRef.current.flyTo([selectedPlace.lat, selectedPlace.lng], 6, { duration: 1.5 });
    }
  }, [selectedPlace]);

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%', minHeight: '400px' }}>
      <div
        ref={containerRef}
        style={{ width: '100%', height: '100%', borderRadius: '12px', overflow: 'hidden', background: '#12121a' }}
      />
      {tileError && (
        <div style={{
          position: 'absolute', bottom: 10, left: 10, right: 10,
          background: 'rgba(0,0,0,0.8)', color: '#aaa', padding: '6px 12px',
          borderRadius: '6px', fontSize: '11px', textAlign: 'center',
        }}>
          Map tiles unavailable — showing markers only
        </div>
      )}
    </div>
  );
}
