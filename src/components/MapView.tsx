import { useEffect, useRef } from 'react';
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

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    mapRef.current = L.map(containerRef.current, {
      center: [20, 0],
      zoom: 2,
      zoomControl: false,
      attributionControl: false,
    });

    // Dark tile layer (CartoDB Dark Matter - free, no key)
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      maxZoom: 19,
    }).addTo(mapRef.current);

    L.control.zoom({ position: 'bottomright' }).addTo(mapRef.current);

    markersRef.current = L.layerGroup().addTo(mapRef.current);

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
        html: `<div style="
          width: ${isSelected ? '16px' : '10px'};
          height: ${isSelected ? '16px' : '10px'};
          background: ${isSelected ? '#22d3ee' : color};
          border-radius: 50%;
          border: 2px solid white;
          box-shadow: 0 0 ${isSelected ? '12px' : '6px'} ${isSelected ? '#22d3ee' : color};
          transition: all 0.2s;
        "></div>`,
        iconSize: [isSelected ? 16 : 10, isSelected ? 16 : 10],
        iconAnchor: [isSelected ? 8 : 5, isSelected ? 8 : 5],
      });

      const marker = L.marker([place.lat, place.lng], { icon })
        .addTo(markersRef.current!)
        .bindPopup(`
          <div style="font-family: system-ui; min-width: 120px;">
            <strong style="font-size: 14px;">${place.name}</strong><br/>
            <span style="color: #666; font-size: 12px;">${place.country}</span><br/>
            ${place.visited ? '<span style="color: #f59e0b; font-size: 11px;">✓ Visited</span>' : '<span style="color: #3b82f6; font-size: 11px;">○ Planned</span>'}
          </div>
        `);

      marker.on('click', () => onPlaceSelect?.(place));
    });
  }, [places, selectedPlace, onPlaceSelect]);

  useEffect(() => {
    if (selectedPlace && mapRef.current) {
      mapRef.current.flyTo([selectedPlace.lat, selectedPlace.lng], 6, { duration: 1.5 });
    }
  }, [selectedPlace]);

  return (
    <div
      ref={containerRef}
      style={{ width: '100%', height: '100%', minHeight: '400px', borderRadius: '12px', overflow: 'hidden' }}
    />
  );
}
