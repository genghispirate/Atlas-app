import { openDB, type IDBPDatabase } from 'idb';
import { v4 as uuidv4 } from 'uuid';

export interface Place {
  id: string;
  name: string;
  lat: number;
  lng: number;
  country: string;
  notes: string;
  visited: boolean;
  visitedDate?: string;
  rating: number;
  tags: string[];
  photoIds: string[];
  createdAt: string;
}

export interface Trip {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  placeIds: string[];
  notes: string;
  coverPhotoId?: string;
  createdAt: string;
}

export interface Photo {
  id: string;
  dataUrl: string;
  placeId?: string;
  tripId?: string;
  caption: string;
  takenAt: string;
  lat?: number;
  lng?: number;
  createdAt: string;
}

export interface AppState {
  places: Place[];
  trips: Trip[];
  photos: Photo[];
  settings: {
    theme: 'dark' | 'light';
    userName: string;
    mapStyle: 'satellite' | 'street' | 'terrain';
  };
}

const DB_NAME = 'atlas-db';
const DB_VERSION = 1;

let dbPromise: Promise<IDBPDatabase> | null = null;

function getDB() {
  if (!dbPromise) {
    dbPromise = openDB(DB_NAME, DB_VERSION, {
      upgrade(db) {
        if (!db.objectStoreNames.contains('places')) {
          db.createObjectStore('places', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('trips')) {
          db.createObjectStore('trips', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('photos')) {
          db.createObjectStore('photos', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('state')) {
          db.createObjectStore('state');
        }
      },
    });
  }
  return dbPromise;
}

export async function getAllPlaces(): Promise<Place[]> {
  const db = await getDB();
  return db.getAll('places');
}

export async function addPlace(place: Omit<Place, 'id' | 'createdAt'>): Promise<Place> {
  const newPlace: Place = {
    ...place,
    id: uuidv4(),
    createdAt: new Date().toISOString(),
  };
  const db = await getDB();
  await db.put('places', newPlace);
  return newPlace;
}

export async function updatePlace(place: Place): Promise<void> {
  const db = await getDB();
  await db.put('places', place);
}

export async function deletePlace(id: string): Promise<void> {
  const db = await getDB();
  await db.delete('places', id);
}

export async function getAllTrips(): Promise<Trip[]> {
  const db = await getDB();
  return db.getAll('trips');
}

export async function addTrip(trip: Omit<Trip, 'id' | 'createdAt'>): Promise<Trip> {
  const newTrip: Trip = {
    ...trip,
    id: uuidv4(),
    createdAt: new Date().toISOString(),
  };
  const db = await getDB();
  await db.put('trips', newTrip);
  return newTrip;
}

export async function updateTrip(trip: Trip): Promise<void> {
  const db = await getDB();
  await db.put('trips', trip);
}

export async function deleteTrip(id: string): Promise<void> {
  const db = await getDB();
  await db.delete('trips', id);
}

export async function getAllPhotos(): Promise<Photo[]> {
  const db = await getDB();
  return db.getAll('photos');
}

export async function addPhoto(photo: Omit<Photo, 'id' | 'createdAt'>): Promise<Photo> {
  const newPhoto: Photo = {
    ...photo,
    id: uuidv4(),
    createdAt: new Date().toISOString(),
  };
  const db = await getDB();
  await db.put('photos', newPhoto);
  return newPhoto;
}

export async function deletePhoto(id: string): Promise<void> {
  const db = await getDB();
  await db.delete('photos', id);
}

export async function getAppState(): Promise<AppState> {
  const db = await getDB();
  const state = await db.get('state', 'appState');
  if (state) return state;
  const defaultState: AppState = {
    places: [],
    trips: [],
    photos: [],
    settings: { theme: 'dark', userName: '', mapStyle: 'satellite' },
  };
  await db.put('state', defaultState, 'appState');
  return defaultState;
}

export async function saveAppState(state: AppState): Promise<void> {
  const db = await getDB();
  await db.put('state', state, 'appState');
}

// Seed data for first launch
export async function seedDemoData(): Promise<void> {
  const state = await getAppState();
  if (state.places.length > 0) return;

  const demoPlaces: Place[] = [
    { id: uuidv4(), name: 'Paris', lat: 48.8566, lng: 2.3522, country: 'France', notes: 'City of Light', visited: true, visitedDate: '2024-06-15', rating: 5, tags: ['europe', 'romance'], photoIds: [], createdAt: new Date().toISOString() },
    { id: uuidv4(), name: 'Tokyo', lat: 35.6762, lng: 139.6503, country: 'Japan', notes: 'Incredible food scene', visited: true, visitedDate: '2024-03-20', rating: 5, tags: ['asia', 'food', 'culture'], photoIds: [], createdAt: new Date().toISOString() },
    { id: uuidv4(), name: 'Marrakech', lat: 31.6295, lng: -7.9811, country: 'Morocco', notes: 'Red city vibes', visited: true, visitedDate: '2023-12-10', rating: 4, tags: ['africa', 'culture', 'desert'], photoIds: [], createdAt: new Date().toISOString() },
    { id: uuidv4(), name: 'New York', lat: 40.7128, lng: -74.006, country: 'USA', notes: 'Never sleeps', visited: true, visitedDate: '2024-01-05', rating: 4, tags: ['america', 'city', 'food'], photoIds: [], createdAt: new Date().toISOString() },
    { id: uuidv4(), name: 'Cape Town', lat: -33.9249, lng: 18.4241, country: 'South Africa', notes: 'Table Mountain is unreal', visited: false, rating: 0, tags: ['africa', 'nature', 'ocean'], photoIds: [], createdAt: new Date().toISOString() },
    { id: uuidv4(), name: 'Reykjavik', lat: 64.1466, lng: -21.9426, country: 'Iceland', notes: 'Northern lights bucket list', visited: false, rating: 0, tags: ['europe', 'nature', 'cold'], photoIds: [], createdAt: new Date().toISOString() },
    { id: uuidv4(), name: 'Buenos Aires', lat: -34.6037, lng: -58.3816, country: 'Argentina', notes: 'Steak & tango', visited: false, rating: 0, tags: ['america', 'culture', 'food'], photoIds: [], createdAt: new Date().toISOString() },
    { id: uuidv4(), name: 'Bali', lat: -8.3405, lng: 115.092, country: 'Indonesia', notes: 'Tropical paradise', visited: true, visitedDate: '2024-08-12', rating: 5, tags: ['asia', 'beach', 'nature'], photoIds: [], createdAt: new Date().toISOString() },
  ];

  const newState: AppState = { ...state, places: demoPlaces };
  await saveAppState(newState);
}
