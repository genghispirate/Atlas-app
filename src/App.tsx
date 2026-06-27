import { useState, useEffect, useCallback } from 'react';
import Globe from './components/Globe';
import MapView from './components/MapView';
import PlaceDetail from './components/PlaceDetail';
import { getAllPlaces, addPlace, updatePlace, deletePlace, seedDemoData } from './lib/db';
import type { Place } from './lib/db';
import './App.css';

type Screen = 'globe' | 'map' | 'places' | 'trips' | 'profile' | 'add-place' | 'place-detail';

export default function App() {
  const [screen, setScreen] = useState<Screen>('globe');
  const [places, setPlaces] = useState<Place[]>([]);
  const [selectedPlace, setSelectedPlace] = useState<Place | null>(null);
  const [viewMode, setViewMode] = useState<'globe' | 'map'>('globe');
  const [loading, setLoading] = useState(true);

  const loadPlaces = useCallback(async () => {
    await seedDemoData();
    const allPlaces = await getAllPlaces();
    setPlaces(allPlaces);
    setLoading(false);
  }, []);

  useEffect(() => {
    loadPlaces();
  }, [loadPlaces]);

  const handleSavePlace = async (placeData: Omit<Place, 'id' | 'createdAt'>) => {
    if (selectedPlace) {
      await updatePlace({ ...placeData, id: selectedPlace.id, createdAt: selectedPlace.createdAt });
    } else {
      await addPlace(placeData);
    }
    await loadPlaces();
    setSelectedPlace(null);
    setScreen('places');
  };

  const handleDeletePlace = async (id: string) => {
    await deletePlace(id);
    await loadPlaces();
    setSelectedPlace(null);
    setScreen('places');
  };

  const handleSelectPlace = (place: Place) => {
    setSelectedPlace(place);
    if (screen === 'globe' || screen === 'map') {
      // Show inline info, don't navigate
    } else {
      setScreen('place-detail');
    }
  };

  const visitedCount = places.filter(p => p.visited).length;
  const plannedCount = places.filter(p => !p.visited).length;

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="loader" />
        <p>Loading Atlas...</p>
      </div>
    );
  }

  return (
    <div className="app">
      {/* Main Content */}
      <div className="main-content">
        {screen === 'globe' && (
          <div className="globe-screen">
            <div className="globe-header">
              <h1>Atlas</h1>
              <div className="view-toggle">
                <button
                  className={viewMode === 'globe' ? 'active' : ''}
                  onClick={() => setViewMode('globe')}
                >
                  🌍 Globe
                </button>
                <button
                  className={viewMode === 'map' ? 'active' : ''}
                  onClick={() => setViewMode('map')}
                >
                  🗺️ Map
                </button>
              </div>
            </div>
            <div className="globe-container">
              {viewMode === 'globe' ? (
                <Globe
                  places={places}
                  onPlaceSelect={handleSelectPlace}
                  selectedPlace={selectedPlace}
                />
              ) : (
                <MapView
                  places={places}
                  onPlaceSelect={handleSelectPlace}
                  selectedPlace={selectedPlace}
                />
              )}
            </div>
            {/* Selected place card */}
            {selectedPlace && (
              <div className="place-card-overlay">
                <div className="place-card">
                  <div className="place-card-header">
                    <h3>{selectedPlace.name}</h3>
                    <span className={`badge ${selectedPlace.visited ? 'visited' : 'planned'}`}>
                      {selectedPlace.visited ? '✓ Visited' : '○ Planned'}
                    </span>
                  </div>
                  <p className="place-country">{selectedPlace.country}</p>
                  {selectedPlace.notes && <p className="place-notes">{selectedPlace.notes}</p>}
                  <div className="place-card-actions">
                    <button className="btn-primary" onClick={() => setScreen('place-detail')}>
                      View Details
                    </button>
                    <button className="btn-ghost" onClick={() => setSelectedPlace(null)}>
                      Close
                    </button>
                  </div>
                </div>
              </div>
            )}
            {/* Stats bar */}
            <div className="stats-bar">
              <div className="stat">
                <span className="stat-number">{places.length}</span>
                <span className="stat-label">Places</span>
              </div>
              <div className="stat">
                <span className="stat-number">{visitedCount}</span>
                <span className="stat-label">Visited</span>
              </div>
              <div className="stat">
                <span className="stat-number">{plannedCount}</span>
                <span className="stat-label">Planned</span>
              </div>
            </div>
          </div>
        )}

        {screen === 'places' && (
          <div className="screen">
            <div className="screen-header">
              <h1>My Places</h1>
              <button className="btn-primary" onClick={() => { setSelectedPlace(null); setScreen('add-place'); }}>
                + Add
              </button>
            </div>
            <div className="places-list">
              {places.length === 0 ? (
                <div className="empty-state">
                  <p>No places yet. Start building your atlas!</p>
                  <button className="btn-primary" onClick={() => setScreen('add-place')}>
                    Add Your First Place
                  </button>
                </div>
              ) : (
                places.map(place => (
                  <div
                    key={place.id}
                    className="place-list-item"
                    onClick={() => { setSelectedPlace(place); setScreen('place-detail'); }}
                  >
                    <div className="place-marker">
                      <div className={`pin ${place.visited ? 'visited' : 'planned'}`} />
                    </div>
                    <div className="place-info">
                      <h3>{place.name}</h3>
                      <p>{place.country} {place.visited ? '✓' : '○'}</p>
                      {place.rating > 0 && <span className="stars">{'★'.repeat(place.rating)}</span>}
                    </div>
                    <div className="place-tags">
                      {place.tags.slice(0, 2).map(tag => (
                        <span key={tag} className="tag">{tag}</span>
                      ))}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {screen === 'add-place' && (
          <PlaceDetail
            place={null}
            onSave={handleSavePlace}
            onCancel={() => setScreen('places')}
          />
        )}

        {screen === 'place-detail' && selectedPlace && (
          <div className="screen slide-in">
            <div className="screen-header">
              <button className="btn-ghost" onClick={() => setScreen('places')}>← Back</button>
              <h2>{selectedPlace.name}</h2>
              <button className="btn-ghost" onClick={() => {}}>⋯</button>
            </div>
            <div className="place-detail">
              <div className="detail-header">
                <span className={`badge large ${selectedPlace.visited ? 'visited' : 'planned'}`}>
                  {selectedPlace.visited ? '✓ Visited' : '○ Planned'}
                </span>
                {selectedPlace.visitedDate && (
                  <span className="date">{selectedPlace.visitedDate}</span>
                )}
              </div>
              <p className="detail-country">{selectedPlace.country}</p>
              {selectedPlace.rating > 0 && (
                <div className="rating-display">
                  {'★'.repeat(selectedPlace.rating)}{'☆'.repeat(5 - selectedPlace.rating)}
                </div>
              )}
              {selectedPlace.notes && (
                <div className="detail-section">
                  <h4>Notes</h4>
                  <p>{selectedPlace.notes}</p>
                </div>
              )}
              {selectedPlace.tags.length > 0 && (
                <div className="detail-section">
                  <h4>Tags</h4>
                  <div className="tags-container">
                    {selectedPlace.tags.map(tag => (
                      <span key={tag} className="tag">{tag}</span>
                    ))}
                  </div>
                </div>
              )}
              <div className="detail-section">
                <h4>Coordinates</h4>
                <p className="coords">{selectedPlace.lat.toFixed(4)}, {selectedPlace.lng.toFixed(4)}</p>
              </div>
              <div className="detail-actions">
                <button className="btn-primary" onClick={() => setScreen('add-place')}>
                  Edit Place
                </button>
                <button className="btn-danger" onClick={() => handleDeletePlace(selectedPlace.id)}>
                  Delete
                </button>
              </div>
            </div>
          </div>
        )}

        {screen === 'trips' && (
          <div className="screen">
            <div className="screen-header">
              <h1>Trips</h1>
              <button className="btn-primary" onClick={() => {}}>+ New</button>
            </div>
            <div className="empty-state">
              <div className="empty-icon">✈️</div>
              <h3>Trip Planning</h3>
              <p>Organize your places into trips. Coming soon!</p>
            </div>
          </div>
        )}

        {screen === 'profile' && (
          <div className="screen">
            <div className="screen-header">
              <h1>Profile</h1>
            </div>
            <div className="profile-screen">
              <div className="profile-avatar">🗺️</div>
              <h2>Traveler</h2>
              <div className="profile-stats">
                <div className="profile-stat">
                  <span className="big-number">{places.length}</span>
                  <span>Total Places</span>
                </div>
                <div className="profile-stat">
                  <span className="big-number">{visitedCount}</span>
                  <span>Visited</span>
                </div>
                <div className="profile-stat">
                  <span className="big-number">{plannedCount}</span>
                  <span>Bucket List</span>
                </div>
              </div>
              <div className="profile-section">
                <h3>Countries Visited</h3>
                <div className="countries-grid">
                  {[...new Set(places.filter(p => p.visited).map(p => p.country))].map(country => (
                    <div key={country} className="country-badge">{country}</div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Bottom Navigation */}
      <nav className="bottom-nav">
        <button
          className={`nav-item ${screen === 'globe' ? 'active' : ''}`}
          onClick={() => setScreen('globe')}
        >
          <span className="nav-icon">🌍</span>
          <span className="nav-label">Globe</span>
        </button>
        <button
          className={`nav-item ${screen === 'places' ? 'active' : ''}`}
          onClick={() => setScreen('places')}
        >
          <span className="nav-icon">📍</span>
          <span className="nav-label">Places</span>
        </button>
        <button
          className={`nav-item ${screen === 'trips' ? 'active' : ''}`}
          onClick={() => setScreen('trips')}
        >
          <span className="nav-icon">✈️</span>
          <span className="nav-label">Trips</span>
        </button>
        <button
          className={`nav-item ${screen === 'profile' ? 'active' : ''}`}
          onClick={() => setScreen('profile')}
        >
          <span className="nav-icon">👤</span>
          <span className="nav-label">Profile</span>
        </button>
      </nav>
    </div>
  );
}
