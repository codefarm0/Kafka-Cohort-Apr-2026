import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { createProduct, getProduct, replaceProduct } from '../../api/ecomApi';

export function AdminProductFormPage() {
  const { productId } = useParams();
  const isNew = !productId || productId === 'new';
  const navigate = useNavigate();

  const [productIdField, setProductIdField] = useState('');
  const [sku, setSku] = useState('');
  const [productName, setProductName] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [price, setPrice] = useState('0');
  const [availableQuantity, setAvailableQuantity] = useState('0');
  const [reservedQuantity, setReservedQuantity] = useState('0');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (isNew) return;
    let cancelled = false;
    (async () => {
      try {
        const p = await getProduct(productId!);
        if (cancelled) return;
        setProductIdField(p.productId);
        setSku(p.sku ?? '');
        setProductName(p.productName);
        setDescription(p.description ?? '');
        setCategoryId(p.categoryId ?? '');
        setPrice(String(p.price));
        setAvailableQuantity(String(p.availableQuantity));
        setReservedQuantity(String(p.reservedQuantity ?? 0));
      } catch (e) {
        if (!cancelled)
          setErr(e instanceof Error ? e.message : 'Failed to load');
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [isNew, productId]);

  return (
    <div>
      <h1>{isNew ? 'New product' : `Edit ${productId}`}</h1>
      <Link to="/admin/products">← Products</Link>
      {err && <div className="error-banner">{err}</div>}
      <form
        style={{ marginTop: '1rem' }}
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setErr(null);
          try {
            if (isNew) {
              await createProduct({
                productId: productIdField.trim(),
                sku: sku.trim() || undefined,
                productName: productName.trim(),
                description: description.trim() || undefined,
                categoryId: categoryId.trim() || undefined,
                price: Number(price),
                quantity: Number(availableQuantity),
              });
            } else {
              await replaceProduct(productId!, {
                sku: sku.trim() || undefined,
                productName: productName.trim(),
                description: description.trim() || undefined,
                categoryId: categoryId.trim() || undefined,
                price: Number(price),
                availableQuantity: Number(availableQuantity),
                reservedQuantity: Number(reservedQuantity) || 0,
              });
            }
            navigate('/admin/products');
          } catch (ex) {
            setErr(ex instanceof Error ? ex.message : 'Save failed');
          } finally {
            setBusy(false);
          }
        }}
      >
        {isNew && (
          <>
            <label htmlFor="pid">Product ID (business key)</label>
            <input
              id="pid"
              value={productIdField}
              onChange={(e) => setProductIdField(e.target.value)}
              required
            />
          </>
        )}
        <label htmlFor="sku">SKU</label>
        <input id="sku" value={sku} onChange={(e) => setSku(e.target.value)} />
        <label htmlFor="pn">Name</label>
        <input
          id="pn"
          value={productName}
          onChange={(e) => setProductName(e.target.value)}
          required
        />
        <label htmlFor="desc">Description</label>
        <textarea
          id="desc"
          rows={3}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <label htmlFor="cat">Category ID</label>
        <input
          id="cat"
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value)}
        />
        <label htmlFor="price">Price</label>
        <input
          id="price"
          type="number"
          step="0.01"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
          required
        />
        <label htmlFor="aq">Available quantity</label>
        <input
          id="aq"
          type="number"
          value={availableQuantity}
          onChange={(e) => setAvailableQuantity(e.target.value)}
          required
        />
        {!isNew && (
          <>
            <label htmlFor="rq">Reserved quantity</label>
            <input
              id="rq"
              type="number"
              value={reservedQuantity}
              onChange={(e) => setReservedQuantity(e.target.value)}
            />
          </>
        )}
        <div className="form-actions">
          <button type="submit" className="primary" disabled={busy}>
            {busy ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  );
}
