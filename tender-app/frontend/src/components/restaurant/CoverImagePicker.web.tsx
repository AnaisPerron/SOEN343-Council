import { useRef, useState } from 'react';
type Props = { onSelect: (dataUri: string) => void; onError: (message: string) => void; label?: string; compact?: boolean };

async function prepareImage(file: File): Promise<string> {
  if (!file.type.startsWith('image/')) throw new Error('Choose an image file.');
  if (file.size > 20 * 1024 * 1024) throw new Error('Choose an image under 20 MB.');

  const objectUrl = URL.createObjectURL(file);
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const element = new Image();
      element.onload = () => resolve(element);
      element.onerror = () => reject(new Error('Could not read this image.'));
      element.src = objectUrl;
    });
    const scale = Math.min(1, 1200 / Math.max(image.width, image.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(image.width * scale));
    canvas.height = Math.max(1, Math.round(image.height * scale));
    const context = canvas.getContext('2d');
    if (!context) throw new Error('Could not prepare this image.');
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise<Blob>((resolve, reject) =>
      canvas.toBlob((result) => result ? resolve(result) : reject(new Error('Could not prepare this image.')), 'image/jpeg', 0.72)
    );
    const dataUri = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(new Error('Could not read this image.'));
      reader.readAsDataURL(blob);
    });
    return dataUri;
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

export function CoverImagePicker({ onSelect, onError, label = 'Restaurant cover image', compact = false }: Props) {
  const input = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);

  const select = async (file?: File) => {
    if (!file) return;
    try { onSelect(await prepareImage(file)); }
    catch (error) { onError(error instanceof Error ? error.message : 'Could not read this image.'); }
  };

  if (compact) return <div><button type="button" aria-label="Change restaurant cover image" onClick={() => input.current?.click()} style={{ width: 44, height: 44, borderRadius: 22, border: 0, background: '#fff', color: '#302629', fontSize: 25, cursor: 'pointer' }}>✎</button><input ref={input} type="file" accept="image/*" hidden onChange={(event) => { void select(event.target.files?.[0]); event.target.value = ''; }} /></div>;
  return <div
    onDragOver={(event) => { event.preventDefault(); setDragging(true); }}
    onDragLeave={() => setDragging(false)}
    onDrop={(event) => { event.preventDefault(); setDragging(false); void select(event.dataTransfer.files[0]); }}
    style={{ border: `1px dashed ${dragging ? '#D42F50' : '#CDBFC1'}`, borderRadius: 12, padding: 18, marginTop: 18, textAlign: 'center', background: dragging ? '#FFF3F5' : '#FFF' }}
  >
    <div style={{ color: '#302629', fontWeight: 700, marginBottom: 8 }}>{label}</div>
    <div style={{ color: '#887B7E', fontSize: 13, marginBottom: 12 }}>Drag and drop an image here, or choose a file.</div>
    <button type="button" onClick={() => input.current?.click()} style={{ background: '#D42F50', color: '#FFF', border: 0, borderRadius: 9, padding: '10px 16px', fontWeight: 700, cursor: 'pointer' }}>
      Choose image
    </button>
    <input ref={input} type="file" accept="image/*" hidden onChange={(event) => { void select(event.target.files?.[0]); event.target.value = ''; }} />
  </div>;
}
