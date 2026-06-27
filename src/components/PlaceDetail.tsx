import { useState } from 'react';
import type { Place } from '../lib/db';

interface PlaceDetailProps {
  place: Place | null;
  onSave: (place: Omit<Place, 'id' | 'createdAt'>) => void;
  onCancel: () => void;
  onDelete?: (id: string) => void;
}

export default function PlaceDetail({ place, onSave, onCancel, onDelete }: PlaceDetailProps) {
  const [name, setName] = useState(place?.name || '');
  const [country, setCountry] = useState(place?.country || '');
  const [lat, setLat] = useState(place?.lat?.toString() || '');
  const [lng, setLng] = useState(place?.lng?.toString() || '');
  const [notes, setNotes] = useState(place?.notes || '');
  const [rating, setRating] = useState(place?.rating || 0);
  const [tags, setTags] = useState(place?.tags?.join(', ') || '');
  const [visited, setVisited] = useState(place?.visited || false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !lat || !lng) return;
    onSave({
      name,
      country,
      lat: parseFloat(lat),
      lng: parseFloat(lng),
      notes,
      rating,
      tags: tags.split(',').map(t => t.trim()).filter(Boolean),
      visited,
      photoIds: place?.photoIds || [],
      visitedDate: visited && !place?.visitedDate ? new Date().toISOString().split('T')[0] : place?.visitedDate,
    });
  };

  return (
    <div className="screen slide-in">
      <div className="screen-header">
        <button className="btn-ghost" onClick={onCancel}>← Back</button>
        <h2>{place ? 'Edit Place' : 'New Place'}</h2>
        {place && onDelete && (
          <button className="btn-danger" onClick={() => onDelete(place.id)}>Delete</button>
        )}
      </div>
      <form onSubmit={handleSubmit} className="form">
        <div className="form-group">
          <label>Place Name</label>
          <input type="text" value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Paris" required />
        </div>
        <div className="form-group">
          <label>Country</label>
          <input type="text" value={country} onChange={e => setCountry(e.target.value)} placeholder="e.g. France" />
        </div>
        <div className="form-row">
          <div className="form-group">
            <label>Latitude</label>
            <input type="number" step="any" value={lat} onChange={e => setLat(e.target.value)} placeholder="48.8566" required />
          </div>
          <div className="form-group">
            <label>Longitude</label>
            <input type="number" step="any" value={lng} onChange={e => setLng(e.target.value)} placeholder="2.3522" required />
          </div>
        </div>
        <div className="form-group">
          <label>Notes</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder="Your memories, tips..." rows={3} />
        </div>
        <div className="form-group">
          <label>Tags (comma separated)</label>
          <input type="text" value={tags} onChange={e => setTags(e.target.value)} placeholder="europe, food, culture" />
        </div>
        <div className="form-group">
          <label>Rating</label>
          <div className="rating">
            {[1, 2, 3, 4, 5].map(n => (
              <button
                key={n}
                type="button"
                className={`star ${n <= rating ? 'active' : ''}`}
                onClick={() => setRating(n)}
              >
                ★
              </button>
            ))}
          </div>
        </div>
        <div className="form-group">
          <label className="checkbox-label">
            <input type="checkbox" checked={visited} onChange={e => setVisited(e.target.checked)} />
            <span>Visited</span>
          </label>
        </div>
        <button type="submit" className="btn-primary">
          {place ? 'Save Changes' : 'Add Place'}
        </button>
      </form>
    </div>
  );
}
