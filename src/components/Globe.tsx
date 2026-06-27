import { useRef, useMemo, useState, useEffect } from 'react';
import { Canvas, useFrame, useLoader } from '@react-three/fiber';
import { OrbitControls, Stars, Html } from '@react-three/drei';
import * as THREE from 'three';
import type { Place } from '../lib/db';

// Earth texture from public domain NASA imagery
const EARTH_TEXTURE = 'https://unpkg.com/three-globe@2.31.1/example/img/earth-blue-marble.jpg';
const EARTH_BUMP = 'https://unpkg.com/three-globe@2.31.1/example/img/earth-topology.png';
const EARTH_SPECULAR = 'https://unpkg.com/three-globe@2.31.1/example/img/earth-water.png';
const EARTH_CLOUDS = 'https://unpkg.com/three-globe@2.31.1/example/img/earth-clouds.png';
const NIGHT_LIGHTS = 'https://unpkg.com/three-globe@2.31.1/example/img/earth-night.jpg';

interface GlobeProps {
  places: Place[];
  onPlaceSelect?: (place: Place) => void;
  selectedPlace?: Place | null;
}

function latLngToVector3(lat: number, lng: number, radius: number): THREE.Vector3 {
  const phi = (90 - lat) * (Math.PI / 180);
  const theta = (lng + 180) * (Math.PI / 180);
  const x = -(radius * Math.sin(phi) * Math.cos(theta));
  const z = radius * Math.sin(phi) * Math.sin(theta);
  const y = radius * Math.cos(phi);
  return new THREE.Vector3(x, y, z);
}

function EarthMesh({ places, onPlaceSelect, selectedPlace }: GlobeProps) {
  const earthRef = useRef<THREE.Mesh>(null);
  const cloudsRef = useRef<THREE.Mesh>(null);
  const [hovered, setHovered] = useState<string | null>(null);

  const textures = useLoader(THREE.TextureLoader, [EARTH_TEXTURE, EARTH_BUMP, EARTH_CLOUDS, NIGHT_LIGHTS]);

  const earthTexture = textures[0];
  const bumpTexture = textures[1];
  const cloudTexture = textures[2];
  const nightTexture = textures[3];

  useFrame((_, delta) => {
    if (earthRef.current) {
      earthRef.current.rotation.y += delta * 0.05;
    }
    if (cloudsRef.current) {
      cloudsRef.current.rotation.y += delta * 0.07;
    }
  });

  const placeMarkers = useMemo(() => {
    return places.map((place) => ({
      place,
      position: latLngToVector3(place.lat, place.lng, 2.02),
      color: place.visited ? '#f59e0b' : '#3b82f6',
    }));
  }, [places]);

  return (
    <group>
      {/* Earth sphere */}
      <mesh ref={earthRef}>
        <sphereGeometry args={[2, 64, 64]} />
        <meshPhongMaterial
          map={earthTexture}
          bumpMap={bumpTexture}
          bumpScale={0.02}
          specularMap={nightTexture}
          specular={new THREE.Color(0x333333)}
          shininess={5}
        />
      </mesh>

      {/* Clouds layer */}
      <mesh ref={cloudsRef}>
        <sphereGeometry args={[2.01, 64, 64]} />
        <meshPhongMaterial
          map={cloudTexture}
          transparent
          opacity={0.35}
          depthWrite={false}
        />
      </mesh>

      {/* Atmosphere glow */}
      <mesh>
        <sphereGeometry args={[2.15, 64, 64]} />
        <shaderMaterial
          transparent
          side={THREE.BackSide}
          depthWrite={false}
          uniforms={{
            glowColor: { value: new THREE.Color(0x4da6ff) },
          }}
          vertexShader={`
            varying vec3 vNormal;
            void main() {
              vNormal = normalize(normalMatrix * normal);
              gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
          `}
          fragmentShader={`
            varying vec3 vNormal;
            uniform vec3 glowColor;
            void main() {
              float intensity = pow(0.65 - dot(vNormal, vec3(0.0, 0.0, 1.0)), 2.0);
              gl_FragColor = vec4(glowColor, intensity * 0.6);
            }
          `}
        />
      </mesh>

      {/* Place markers */}
      {placeMarkers.map(({ place, position, color }) => (
        <group key={place.id} position={position}>
          {/* Pin dot */}
          <mesh
            onPointerOver={() => setHovered(place.id)}
            onPointerOut={() => setHovered(null)}
            onClick={() => onPlaceSelect?.(place)}
          >
            <sphereGeometry args={[0.03, 16, 16]} />
            <meshBasicMaterial
              color={selectedPlace?.id === place.id ? '#22d3ee' : color}
            />
          </mesh>

          {/* Vertical beam */}
          <mesh position={[0, 0.06, 0]}>
            <cylinderGeometry args={[0.003, 0.003, 0.12, 8]} />
            <meshBasicMaterial
              color={selectedPlace?.id === place.id ? '#22d3ee' : color}
              transparent
              opacity={0.8}
            />
          </mesh>

          {/* Glow ring for visited */}
          {place.visited && (
            <mesh rotation={[Math.PI / 2, 0, 0]}>
              <ringGeometry args={[0.04, 0.06, 32]} />
              <meshBasicMaterial color="#f59e0b" transparent opacity={0.5} side={THREE.DoubleSide} />
            </mesh>
          )}

          {/* Hover label */}
          {hovered === place.id && (
            <Html distanceFactor={6} style={{ pointerEvents: 'none' }}>
              <div style={{
                background: 'rgba(0,0,0,0.85)',
                color: 'white',
                padding: '4px 10px',
                borderRadius: '6px',
                fontSize: '12px',
                whiteSpace: 'nowrap',
                border: `1px solid ${color}`,
                fontFamily: 'system-ui',
              }}>
                {place.name}
              </div>
            </Html>
          )}
        </group>
      ))}
    </group>
  );
}

export default function Globe({ places, onPlaceSelect, selectedPlace }: GlobeProps) {
  return (
    <div style={{ width: '100%', height: '100%', minHeight: '400px' }}>
      <Canvas
        camera={{ position: [0, 0, 5.5], fov: 45 }}
        style={{ background: 'transparent' }}
      >
        <ambientLight intensity={0.3} />
        <directionalLight position={[5, 3, 5]} intensity={1.2} />
        <pointLight position={[-10, -10, -10]} intensity={0.3} color="#4da6ff" />
        <Stars radius={100} depth={50} count={3000} factor={4} saturation={0} fade speed={1} />
        <EarthMesh places={places} onPlaceSelect={onPlaceSelect} selectedPlace={selectedPlace} />
        <OrbitControls
          enablePan={false}
          minDistance={3}
          maxDistance={10}
          autoRotate
          autoRotateSpeed={0.3}
          enableDamping
          dampingFactor={0.05}
        />
      </Canvas>
    </div>
  );
}
