import { useEffect, useMemo, useState, type FormEvent } from 'react'

type TransactionType = 'INCOME' | 'EXPENSE'

type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  userId: number
  email: string
  fullName: string
}

type Category = {
  id: number
  name: string
  type: TransactionType
}

type Transaction = {
  id: number
  amount: number
  description: string
  type: TransactionType
  transactionDate: string
  categoryId: number | null
  categoryName: string | null
}

type ApiError = {
  timestamp?: string
  status?: number
  message?: string
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'
const AUTH_STORAGE_KEY = 'finance-tracker-auth'

const initialAuthForm = {
  fullName: '',
  email: '',
  password: '',
}

const initialCategoryForm = {
  name: '',
  type: 'EXPENSE' as TransactionType,
}

const initialTransactionForm = {
  amount: '',
  description: '',
  type: 'EXPENSE' as TransactionType,
  transactionDate: new Date().toISOString().slice(0, 10),
  categoryId: '',
}

function App() {
  const [authMode, setAuthMode] = useState<'login' | 'register'>('register')
  const [auth, setAuth] = useState<AuthResponse | null>(() => {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AuthResponse) : null
  })
  const [authForm, setAuthForm] = useState(initialAuthForm)
  const [categoryForm, setCategoryForm] = useState(initialCategoryForm)
  const [transactionForm, setTransactionForm] = useState(initialTransactionForm)
  const [categories, setCategories] = useState<Category[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [filterType, setFilterType] = useState<'ALL' | TransactionType>('ALL')
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Transaction | null>(null)
  const [editForm, setEditForm] = useState(initialTransactionForm)
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (auth) {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth))
      void loadDashboard(auth.accessToken, auth.refreshToken)
    } else {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      setCategories([])
      setTransactions([])
    }
  }, [auth])

  const filteredTransactions = useMemo(() => {
    if (filterType === 'ALL') {
      return transactions
    }
    return transactions.filter((transaction) => transaction.type === filterType)
  }, [filterType, transactions])

  const stats = useMemo(() => {
    const income = transactions
      .filter((transaction) => transaction.type === 'INCOME')
      .reduce((sum, transaction) => sum + transaction.amount, 0)
    const expense = transactions
      .filter((transaction) => transaction.type === 'EXPENSE')
      .reduce((sum, transaction) => sum + transaction.amount, 0)
    return {
      income,
      expense,
      balance: income - expense,
    }
  }, [transactions])

  async function apiFetch<T>(path: string, options: RequestInit = {}, accessToken?: string): Promise<T> {
    const headers = new Headers(options.headers)
    headers.set('Content-Type', 'application/json')

    if (accessToken) {
      headers.set('Authorization', `Bearer ${accessToken}`)
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    })

    if (response.status === 204) {
      return undefined as T
    }

    const responseText = await response.text()
    let data: unknown = null

    if (responseText) {
      try {
        data = JSON.parse(responseText)
      } catch {
        data = responseText
      }
    }

    if (!response.ok) {
      const apiError = typeof data === 'object' && data !== null ? (data as ApiError) : null
      const fallbackMessage = response.status === 0 ? 'Backend ulasilamiyor' : `HTTP ${response.status}`
      throw new Error(apiError?.message ?? (typeof data === 'string' ? data : fallbackMessage))
    }

    return data as T
  }

  async function loadDashboard(accessToken: string, refreshToken: string) {
    setIsLoading(true)
    setError(null)
    try {
      const [loadedCategories, loadedTransactions] = await Promise.all([
        apiFetch<Category[]>('/categories', {}, accessToken),
        apiFetch<Transaction[]>('/transactions', {}, accessToken),
      ])
      setCategories(loadedCategories)
      setTransactions(loadedTransactions)
    } catch (fetchError) {
      if (refreshToken) {
        try {
          const refreshedAuth = await apiFetch<AuthResponse>('/auth/refresh', {
            method: 'POST',
            body: JSON.stringify({ refreshToken }),
          })
          setAuth(refreshedAuth)
          return
        } catch {
          setAuth(null)
        }
      }

      setError(fetchError instanceof Error ? fetchError.message : 'Dashboard verileri yüklenemedi')
    } finally {
      setIsLoading(false)
    }
  }

  async function handleAuthSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setMessage(null)
    setIsLoading(true)

    try {
      const path = authMode === 'register' ? '/auth/register' : '/auth/login'
      const payload =
        authMode === 'register'
          ? authForm
          : { email: authForm.email, password: authForm.password }

      const authResponse = await apiFetch<AuthResponse>(path, {
        method: 'POST',
        body: JSON.stringify(payload),
      })

      setAuth(authResponse)
      setMessage(authMode === 'register' ? 'Hesap oluşturuldu ve giriş yapıldı.' : 'Giriş başarılı.')
      setAuthForm(initialAuthForm)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Kimlik doğrulama başarısız oldu')
    } finally {
      setIsLoading(false)
    }
  }

  async function handleCategorySubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!auth) return

    setError(null)
    setMessage(null)

    try {
      const createdCategory = await apiFetch<Category>(
        '/categories',
        {
          method: 'POST',
          body: JSON.stringify(categoryForm),
        },
        auth.accessToken,
      )

      setCategories((current) => [...current, createdCategory].sort((a, b) => a.name.localeCompare(b.name)))
      setCategoryForm(initialCategoryForm)
      setMessage(`Kategori eklendi: ${createdCategory.name}`)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Kategori eklenemedi')
    }
  }

  async function handleTransactionSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!auth) return

    setError(null)
    setMessage(null)

    try {
      const createdTransaction = await apiFetch<Transaction>(
        '/transactions',
        {
          method: 'POST',
          body: JSON.stringify({
            amount: Number(transactionForm.amount),
            description: transactionForm.description,
            type: transactionForm.type,
            transactionDate: transactionForm.transactionDate,
            categoryId: transactionForm.categoryId ? Number(transactionForm.categoryId) : null,
          }),
        },
        auth.accessToken,
      )

      setTransactions((current) =>
        [...current, createdTransaction].sort((a, b) => b.transactionDate.localeCompare(a.transactionDate)),
      )
      setTransactionForm(initialTransactionForm)
      setMessage(`Islem eklendi: ${createdTransaction.description}`)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Islem eklenemedi')
    }
  }

  function openEditModal(transaction: Transaction) {
    setEditForm({
      amount: String(transaction.amount),
      description: transaction.description,
      type: transaction.type,
      transactionDate: transaction.transactionDate,
      categoryId: transaction.categoryId ? String(transaction.categoryId) : '',
    })
    setEditingTransaction(transaction)
    setError(null)
  }

  function closeEditModal() {
    setEditingTransaction(null)
    setEditForm(initialTransactionForm)
  }

  async function handleEditSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!auth || !editingTransaction) return

    setError(null)
    setMessage(null)

    try {
      const updatedTransaction = await apiFetch<Transaction>(
        `/transactions/${editingTransaction.id}`,
        {
          method: 'PUT',
          body: JSON.stringify({
            amount: Number(editForm.amount),
            description: editForm.description,
            type: editForm.type,
            transactionDate: editForm.transactionDate,
            categoryId: editForm.categoryId ? Number(editForm.categoryId) : null,
          }),
        },
        auth.accessToken,
      )

      setTransactions((current) =>
        current
          .map((transaction) => (transaction.id === updatedTransaction.id ? updatedTransaction : transaction))
          .sort((a, b) => b.transactionDate.localeCompare(a.transactionDate)),
      )
      closeEditModal()
      setMessage(`Islem guncellendi: ${updatedTransaction.description}`)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Islem guncellenemedi')
    }
  }

  async function handleDeleteConfirm() {
    if (!auth || !deleteTarget) return

    setError(null)
    setMessage(null)

    try {
      await apiFetch<void>(`/transactions/${deleteTarget.id}`, { method: 'DELETE' }, auth.accessToken)
      setTransactions((current) => current.filter((transaction) => transaction.id !== deleteTarget.id))
      setMessage(`Islem silindi: ${deleteTarget.description}`)
      setDeleteTarget(null)
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Islem silinemedi')
      setDeleteTarget(null)
    }
  }

  async function handleLogout() {
    if (!auth) return

    try {
      await apiFetch<void>('/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken: auth.refreshToken }),
      })
    } catch {
      // Logout'ta client state'i temizlemek yeterli.
    } finally {
      setAuth(null)
      setMessage('Cikis yapildi.')
    }
  }

  return (
    <div className="app-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">Finance Tracker</p>
          <h1>Kisisel finans akisina tek ekranda bak</h1>
          <p className="hero-copy">
            Gelir ve giderlerini kategorilere ayir, islemlerini duzenle ve net bakiyeni tek bakista takip et.
          </p>
        </div>

        <div className="hero-card">
          <span className="hero-card-label">Durum</span>
          <strong>{auth ? `Hos geldin, ${auth.fullName}` : 'Henüz giris yapilmadi'}</strong>
          <p>{auth ? auth.email : 'Hesabin yoksa kayit ol, varsa giris yaparak basla.'}</p>
          {auth ? (
            <button type="button" className="secondary-button" onClick={handleLogout}>
              Logout
            </button>
          ) : null}
        </div>
      </header>

      {message ? <div className="feedback success">{message}</div> : null}
      {error ? <div className="feedback error">{error}</div> : null}

      {!auth ? (
        <section className="grid">
          <article className="panel">
            <div className="panel-header">
              <h2>{authMode === 'register' ? 'Register' : 'Login'}</h2>
              <button
                type="button"
                className="link-button"
                onClick={() => setAuthMode((current) => (current === 'register' ? 'login' : 'register'))}
              >
                {authMode === 'register' ? 'Login ekranina gec' : 'Register ekranina gec'}
              </button>
            </div>

            <form className="form-grid" onSubmit={handleAuthSubmit}>
              {authMode === 'register' ? (
                <label>
                  Full name
                  <input
                    value={authForm.fullName}
                    onChange={(event) => setAuthForm((current) => ({ ...current, fullName: event.target.value }))}
                    placeholder="Jarvis Developer"
                    required
                  />
                </label>
              ) : null}

              <label>
                Email
                <input
                  type="email"
                  value={authForm.email}
                  onChange={(event) => setAuthForm((current) => ({ ...current, email: event.target.value }))}
                  placeholder="jarvis@example.com"
                  required
                />
              </label>

              <label>
                Password
                <input
                  type="password"
                  value={authForm.password}
                  onChange={(event) => setAuthForm((current) => ({ ...current, password: event.target.value }))}
                  placeholder="minimum 8 karakter"
                  required
                />
              </label>

              <button type="submit" disabled={isLoading}>
                {authMode === 'register' ? 'Register + Login' : 'Login'}
              </button>
            </form>
          </article>
        </section>
      ) : null}

      <section className="stats-grid">
        <article className="stat-card">
          <span>Toplam gelir</span>
          <strong>{formatCurrency(stats.income)}</strong>
        </article>
        <article className="stat-card">
          <span>Toplam gider</span>
          <strong>{formatCurrency(stats.expense)}</strong>
        </article>
        <article className="stat-card">
          <span>Net bakiye</span>
          <strong>{formatCurrency(stats.balance)}</strong>
        </article>
      </section>

      <section className="grid two-columns">
        <article className="panel">
          <div className="panel-header">
            <h2>Kategori ekle</h2>
          </div>
          <form className="form-grid" onSubmit={handleCategorySubmit}>
            <label>
              Kategori adi
              <input
                value={categoryForm.name}
                onChange={(event) => setCategoryForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Ulasim"
                disabled={!auth}
                required
              />
            </label>

            <label>
              Tip
              <select
                value={categoryForm.type}
                onChange={(event) =>
                  setCategoryForm((current) => ({ ...current, type: event.target.value as TransactionType }))
                }
                disabled={!auth}
              >
                <option value="EXPENSE">Expense</option>
                <option value="INCOME">Income</option>
              </select>
            </label>

            <button type="submit" disabled={!auth}>
              Kategori olustur
            </button>
          </form>

          <div className="chip-list">
            {categories.map((category) => (
              <span key={category.id} className="chip">
                {category.name} · {category.type}
              </span>
            ))}
          </div>
        </article>

        <article className="panel">
          <div className="panel-header">
            <h2>Transaction ekle</h2>
          </div>
          <form className="form-grid" onSubmit={handleTransactionSubmit}>
            <label>
              Tutar
              <input
                type="number"
                min="0"
                step="0.01"
                value={transactionForm.amount}
                onChange={(event) =>
                  setTransactionForm((current) => ({ ...current, amount: event.target.value }))
                }
                placeholder="1250.00"
                disabled={!auth}
                required
              />
            </label>

            <label>
              Aciklama
              <input
                value={transactionForm.description}
                onChange={(event) =>
                  setTransactionForm((current) => ({ ...current, description: event.target.value }))
                }
                placeholder="Aylik maas / market"
                disabled={!auth}
                required
              />
            </label>

            <label>
              Tip
              <select
                value={transactionForm.type}
                onChange={(event) =>
                  setTransactionForm((current) => ({ ...current, type: event.target.value as TransactionType }))
                }
                disabled={!auth}
              >
                <option value="EXPENSE">Expense</option>
                <option value="INCOME">Income</option>
              </select>
            </label>

            <label>
              Tarih
              <input
                type="date"
                value={transactionForm.transactionDate}
                onChange={(event) =>
                  setTransactionForm((current) => ({ ...current, transactionDate: event.target.value }))
                }
                disabled={!auth}
                required
              />
            </label>

            <label>
              Kategori
              <select
                value={transactionForm.categoryId}
                onChange={(event) =>
                  setTransactionForm((current) => ({ ...current, categoryId: event.target.value }))
                }
                disabled={!auth}
              >
                <option value="">Kategori sec</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>

            <button type="submit" disabled={!auth}>
              Transaction olustur
            </button>
          </form>
        </article>
      </section>

      <section className="panel">
        <div className="panel-header">
          <h2>Transaction listesi</h2>
          <select value={filterType} onChange={(event) => setFilterType(event.target.value as 'ALL' | TransactionType)}>
            <option value="ALL">Tum tipler</option>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
          </select>
        </div>

        {isLoading ? <p>Yukleniyor...</p> : null}

        <div className="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Tarih</th>
                <th>Aciklama</th>
                <th>Kategori</th>
                <th>Tip</th>
                <th>Tutar</th>
                <th>Islemler</th>
              </tr>
            </thead>
            <tbody>
              {filteredTransactions.length === 0 ? (
                <tr>
                  <td colSpan={6} className="empty-state">
                    Henüz transaction yok.
                  </td>
                </tr>
              ) : (
                filteredTransactions.map((transaction) => (
                  <tr key={transaction.id}>
                    <td>{transaction.transactionDate}</td>
                    <td>{transaction.description}</td>
                    <td>{transaction.categoryName ?? '-'}</td>
                    <td>
                      <span className={`tag ${transaction.type.toLowerCase()}`}>{transaction.type}</span>
                    </td>
                    <td>{formatCurrency(transaction.amount)}</td>
                    <td>
                      <div className="row-actions">
                        <button
                          type="button"
                          className="icon-button edit"
                          onClick={() => openEditModal(transaction)}
                          disabled={!auth}
                        >
                          Duzenle
                        </button>
                        <button
                          type="button"
                          className="icon-button delete"
                          onClick={() => setDeleteTarget(transaction)}
                          disabled={!auth}
                        >
                          Sil
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {editingTransaction ? (
        <div className="modal-backdrop" onClick={closeEditModal}>
          <div className="modal" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true">
            <h3>Transaction duzenle</h3>
            <p className="modal-copy">{editingTransaction.description}</p>
            <form className="form-grid" onSubmit={handleEditSubmit}>
              <label>
                Tutar
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={editForm.amount}
                  onChange={(event) => setEditForm((current) => ({ ...current, amount: event.target.value }))}
                  required
                />
              </label>

              <label>
                Aciklama
                <input
                  value={editForm.description}
                  onChange={(event) => setEditForm((current) => ({ ...current, description: event.target.value }))}
                  required
                />
              </label>

              <label>
                Tip
                <select
                  value={editForm.type}
                  onChange={(event) =>
                    setEditForm((current) => ({ ...current, type: event.target.value as TransactionType }))
                  }
                >
                  <option value="EXPENSE">Expense</option>
                  <option value="INCOME">Income</option>
                </select>
              </label>

              <label>
                Tarih
                <input
                  type="date"
                  value={editForm.transactionDate}
                  onChange={(event) =>
                    setEditForm((current) => ({ ...current, transactionDate: event.target.value }))
                  }
                  required
                />
              </label>

              <label>
                Kategori
                <select
                  value={editForm.categoryId}
                  onChange={(event) => setEditForm((current) => ({ ...current, categoryId: event.target.value }))}
                >
                  <option value="">Kategori sec</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </label>

              <div className="modal-actions">
                <button type="button" className="secondary-button" onClick={closeEditModal}>
                  Iptal
                </button>
                <button type="submit">Kaydet</button>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      {deleteTarget ? (
        <div className="modal-backdrop" onClick={() => setDeleteTarget(null)}>
          <div className="modal" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true">
            <h3>Transaction silinsin mi?</h3>
            <p className="modal-copy">
              <strong>{deleteTarget.description}</strong> islemini silmek istediginize emin misiniz?
            </p>
            <div className="modal-actions">
              <button type="button" className="secondary-button" onClick={() => setDeleteTarget(null)}>
                Vazgec
              </button>
              <button type="button" className="danger-button" onClick={() => void handleDeleteConfirm()}>
                Evet, sil
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    maximumFractionDigits: 2,
  }).format(value)
}

export default App
