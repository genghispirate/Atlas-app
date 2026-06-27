import { useRef, useMemo, useState } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, Stars, Html } from '@react-three/drei';
import * as THREE from 'three';
import type { Place } from '../lib/db';

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

// Procedural earth-like texture using canvas
function createEarthTexture(): THREE.CanvasTexture {
  const canvas = document.createElement('canvas');
  canvas.width = 2048;
  canvas.height = 1024;
  const ctx = canvas.getContext('2d')!;

  // Ocean gradient
  const oceanGrad = ctx.createLinearGradient(0, 0, 0, canvas.height);
  oceanGrad.addColorStop(0, '#1a3a5c');
  oceanGrad.addColorStop(0.3, '#1e4d7a');
  oceanGrad.addColorStop(0.5, '#1565a0');
  oceanGrad.addColorStop(0.7, '#1e4d7a');
  oceanGrad.addColorStop(1, '#1a3a5c');
  ctx.fillStyle = oceanGrad;
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // Simplified continent shapes (lat/lng → pixel mapping)
  const continents = [
    // North America
    { points: [[-130,50],[-125,40],[-105,30],[-95,25],[-85,30],[-80,40],[-75,45],[-65,50],[-60,55],[-70,60],[-85,65],[-100,70],[-120,65],[-130,55]] },
    // South America
    { points: [[-80,10],[-75,5],[-70,-5],[-65,-10],[-55,-5],[-45,-10],[-38,-15],[-37,-22],[-42,-28],[-48,-33],[-53,-38],[-58,-42],[-65,-50],[-70,-55],[-75,-45],[-75,-35],[-80,-20],[-82,-5],[-80,10]] },
    // Europe
    { points: [[-10,35],[-5,36],[0,38],[5,43],[10,45],[15,42],[20,38],[25,36],[30,38],[35,42],[30,48],[25,52],[20,55],[15,58],[10,60],[5,55],[0,52],[-5,50],[-10,45],[-10,35]] },
    // Africa
    { points: [[-15,30],[-10,35],[0,37],[10,35],[15,32],[20,30],[25,30],[30,28],[33,25],[35,15],[40,10],[45,5],[50,0],[45,-5],[42,-10],[40,-15],[38,-20],[35,-25],[32,-30],[28,-33],[20,-33],[15,-30],[12,-25],[15,-15],[18,-5],[15,5],[10,10],[5,10],[0,10],[-5,10],[-10,15],[-15,20],[-15,30]] },
    // Asia
    { points: [[30,38],[35,42],[40,45],[45,42],[50,40],[55,42],[60,45],[65,40],[70,35],[75,30],[80,28],[85,28],[90,25],[95,20],[100,15],[105,20],[110,22],[115,25],[120,30],[125,35],[130,40],[135,45],[140,48],[145,50],[150,55],[155,60],[160,65],[170,68],[175,65],[170,60],[160,55],[150,50],[140,45],[130,40],[120,35],[110,30],[100,25],[90,22],[80,20],[70,25],[60,30],[50,35],[40,38],[30,38]] },
    // Australia
    { points: [[115,-15],[120,-13],[130,-12],[135,-15],[140,-18],[145,-20],[150,-23],[152,-28],[150,-33],[145,-38],[140,-38],[135,-35],[130,-32],[125,-30],[120,-25],[115,-22],[113,-20],[115,-15]] },
  ];

  // Draw continents
  continents.forEach(continent => {
    ctx.beginPath();
    continent.points.forEach((point, i) => {
      const x = ((point[0] + 180) / 360) * canvas.width;
      const y = ((90 - point[1]) / 180) * canvas.height;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.closePath();

    // Land gradient
    const landGrad = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
    landGrad.addColorStop(0, '#2d5a27');
    landGrad.addColorStop(0.3, '#3a7a32');
    landGrad.addColorStop(0.5, '#4a8a3a');
    landGrad.addColorStop(0.7, '#3a7a32');
    landGrad.addColorStop(1, '#2d5a27');
    ctx.fillStyle = landGrad;
    ctx.fill();

    // Subtle border
    ctx.strokeStyle = 'rgba(100, 180, 80, 0.3)';
    ctx.lineWidth = 2;
    ctx.stroke();
  });

  // Add some noise/texture
  for (let i = 0; i < 5000; i++) {
    const x = Math.random() * canvas.width;
    const y = Math.random() * canvas.height;
    const pixel = ctx.getImageData(Math.floor(x), Math.floor(y), 1, 1).data;
    if (pixel[1] > 80) { // land
      ctx.fillStyle = `rgba(${60 + Math.random() * 40}, ${100 + Math.random() * 60}, ${50 + Math.random() * 30}, 0.3)`;
      ctx.fillRect(x, y, 3, 3);
    }
  }

  // Ice caps
  ctx.fillStyle = 'rgba(220, 230, 240, 0.8)';
  ctx.fillRect(0, 0, canvas.width, 40);
  ctx.fillRect(0, canvas.height - 40, canvas.width, 40);

  const texture = new THREE.CanvasTexture(canvas);
  texture.needsUpdate = true;
  return texture;
}

// Procedural bump map
function createBumpTexture(): THREE.CanvasTexture {
  const canvas = document.createElement('canvas');
  canvas.width = 1024;
  canvas.height = 512;
  const ctx = canvas.getContext('2d')!;

  ctx.fillStyle = '#808080';
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // Add noise for terrain
  for (let i = 0; i < 3000; i++) {
    const x = Math.random() * canvas.width;
    const y = Math.random() * canvas.height;
    const brightness = Math.floor(100 + Math.random() * 60);
    ctx.fillStyle = `rgb(${brightness},${brightness},${brightness})`;
    ctx.fillRect(x, y, 4, 4);
  }

  const texture = new THREE.CanvasTexture(canvas);
  texture.needsUpdate = true;
  return texture;
}

function EarthMesh({ places, onPlaceSelect, selectedPlace }: GlobeProps) {
  const earthRef = useRef<THREE.Mesh>(null);
  const cloudsRef = useRef<THREE.Mesh>(null);
  const [hovered, setHovered] = useState<string | null>(null);

  const earthTexture = useMemo(() => createEarthTexture(), []);
  const bumpTexture = useMemo(() => createBumpTexture(), []);

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
          bumpScale={0.015}
          specular={new THREE.Color(0x222222)}
          shininess={10}
        />
      </mesh>

      {/* Clouds layer - subtle wireframe-like sphere */}
      <mesh ref={cloudsRef}>
        <sphereGeometry args={[2.015, 48, 48]} />
        <meshPhongMaterial
          color="#ffffff"
          transparent
          opacity={0.08}
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
          <mesh
            onPointerOver={() => setHovered(place.id)}
            onPointerOut={() => setHovered(null)}
            onClick={() => onPlaceSelect?.(place)}
          >
            <sphereGeometry args={[0.035, 16, 16]} />
            <meshBasicMaterial
              color={selectedPlace?.id === place.id ? '#22d3ee' : color}
            />
          </mesh>

          <mesh position={[0, 0.06, 0]}>
            <cylinderGeometry args={[0.004, 0.004, 0.12, 8]} />
            <meshBasicMaterial
              color={selectedPlace?.id === place.id ? '#22d3ee' : color}
              transparent
              opacity={0.8}
            />
          </mesh>

          {place.visited && (
            <mesh rotation={[Math.PI / 2, 0, 0]}>
              <ringGeometry args={[0.04, 0.065, 32]} />
              <meshBasicMaterial color="#f59e0b" transparent opacity={0.5} side={THREE.DoubleSide} />
            </mesh>
          )}

          {hovered === place.id && (
            <Html distanceFactor={6} style={{ pointerEvents: 'none' }}>
              <div style={{
                background: 'rgba(0,0,0,0.88)',
                color: 'white',
                padding: '5px 12px',
                borderRadius: '8px',
                fontSize: '13px',
                whiteSpace: 'nowrap',
                border: `1px solid ${color}`,
                fontFamily: 'system-ui',
                fontWeight: 500,
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
        <ambientLight intensity={0.4} />
        <directionalLight position={[5, 3, 5]} intensity={1.1} />
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
