import React, { useState, useEffect, useRef, createContext, useContext, useCallback } from 'react';
import { Routes, Route, Link, useLocation, Navigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Zap, Sparkles, ChevronRight, Briefcase, BrainCircuit, Target, Code,
  ArrowRight, Search, MapPin, Filter, Command, CheckCircle2, User, LogOut,
  Bell, X, Clock, AlertCircle, Info, UserPlus, Rocket,
  Twitter, Linkedin, Heart, Mail,
  Eye, EyeOff, Lock, Shield, Key, Smartphone, GraduationCap, Building2
} from 'lucide-react';

// --- Relative time helper ---
function timeAgo(dateStr: string | null | undefined): string {
  if (!dateStr) return '';
  const now = new Date();
  const date = new Date(dateStr);
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  if (diffMins < 1) return 'Just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays}d ago`;
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}w ago`;
  return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}


// --- Global API URL --- (use env var in production, fallback to localhost for dev)
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8000';

// --- TOAST NOTIFICATION SYSTEM ---
type ToastType = 'success' | 'error' | 'info' | 'warning';
interface Toast {
  id: number;
  message: string;
  type: ToastType;
  exiting?: boolean;
}

interface ToastContextType {
  toast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextType>({ toast: () => { } });
export const useToast = () => useContext(ToastContext);

function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const counterRef = useRef(0);

  const addToast = useCallback((message: string, type: ToastType = 'info') => {
    const id = ++counterRef.current;
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.map(t => t.id === id ? { ...t, exiting: true } : t));
      setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 300);
    }, 3500);
  }, []);

  const removeToast = useCallback((id: number) => {
    setToasts(prev => prev.map(t => t.id === id ? { ...t, exiting: true } : t));
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 300);
  }, []);

  const iconMap: Record<ToastType, React.ReactNode> = {
    success: <CheckCircle2 size={18} />,
    error: <AlertCircle size={18} />,
    info: <Info size={18} />,
    warning: <AlertCircle size={18} />,
  };

  return (
    <ToastContext.Provider value={{ toast: addToast }}>
      {children}
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast toast-${t.type}${t.exiting ? ' toast-exit' : ''}`}>
            {iconMap[t.type]}
            <span style={{ flex: 1 }}>{t.message}</span>
            <button className="toast-close" onClick={() => removeToast(t.id)}><X size={14} /></button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

// --- AUTH CONTEXT ---
interface UserData {
  id: number;
  phone: string;
  name: string;
  role: string;
}

interface AuthContextType {
  token: string | null;
  user: UserData | null;
  setAuthState: (token: string, user: UserData) => void;
  logout: () => void;
  isLoginModalOpen: boolean;
  setLoginModalOpen: (open: boolean) => void;
}

const AuthContext = createContext<AuthContextType>({
  token: null,
  user: null,
  setAuthState: () => { },
  logout: () => { },
  isLoginModalOpen: false,
  setLoginModalOpen: () => { },
});

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'));
  const [user, setUser] = useState<UserData | null>(
    localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')!) : null
  );
  const [isLoginModalOpen, setLoginModalOpen] = useState(false);

  const setAuthState = (newToken: string, newUser: UserData) => {
    setToken(newToken);
    setUser(newUser);
    localStorage.setItem('token', newToken);
    localStorage.setItem('user', JSON.stringify(newUser));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  };

  return (
    <AuthContext.Provider value={{ token, user, setAuthState, logout, isLoginModalOpen, setLoginModalOpen }}>
      {children}
    </AuthContext.Provider>
  );
};

// --- MAIN APP COMPONENT ---
function AppContent() {
  const { user } = useAuth();
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  return (
    <div style={{ position: 'relative' }}>
      <div className="noise" />
      <Navbar />

      <main style={{ minHeight: '100vh', paddingTop: 'var(--nav-height)' }}>
        {mounted && (
          <AnimatePresence mode="wait">
            <Routes>
              <Route path="/" element={<LandingPage />} />
              <Route path="/jobs" element={<JobsPage />} />
              <Route path="/employers" element={user?.role === 'student' ? <Navigate to="/jobs" /> : <EmployersPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Routes>
          </AnimatePresence>
        )}
      </main>

      <Footer />

      {/* Global Modals */}
      <CmdKModal />
      <LoginModal />
    </div>
  );
}

// --- ERROR BOUNDARY ---
class ErrorBoundary extends React.Component<{children: React.ReactNode}, {hasError: boolean, error: Error | null}> {
  constructor(props: {children: React.ReactNode}) {
    super(props);
    this.state = { hasError: false, error: null };
  }
  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }
  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error('ErrorBoundary caught:', error, info);
  }
  render() {
    if (this.state.hasError) {
      return (
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#000', color: '#fff', flexDirection: 'column', gap: '16px', padding: '40px', textAlign: 'center' }}>
          <Zap size={48} color="#8B5CF6" />
          <h1 style={{ fontSize: '28px', fontWeight: 700 }}>Something went wrong</h1>
          <p style={{ color: '#A1A1AA', maxWidth: '400px' }}>An unexpected error occurred. Please refresh the page to continue.</p>
          <button onClick={() => window.location.reload()} style={{ background: 'white', color: 'black', border: 'none', padding: '12px 24px', borderRadius: '99px', fontWeight: 600, cursor: 'pointer', fontSize: '14px' }}>Refresh Page</button>
        </div>
      );
    }
    return this.props.children;
  }
}

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <ToastProvider>
          <AppContent />
        </ToastProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}

// --- NAVBAR COMPONENT ---
function Navbar() {
  const location = useLocation();
  const [scrolled, setScrolled] = useState(false);
  const { user, token, setLoginModalOpen, logout } = useAuth();

  const [unreadCount, setUnreadCount] = useState(0);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  useEffect(() => {
    if (!token) return;

    const fetchUnread = () => {
      fetch(`${API_URL}/api/notifications/unread-count`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
        .then(res => res.json())
        .then(data => setUnreadCount(data.unread_count || 0))
        .catch(() => { });
    };

    fetchUnread();
    const interval = setInterval(fetchUnread, 15000); // Poll every 15s
    return () => clearInterval(interval);
  }, [token]);

  return (
    <>
      <header
        className={scrolled ? 'nav-blur' : ''}
        style={{
          position: 'fixed', top: 0, left: 0, right: 0, zIndex: 100,
          height: 'var(--nav-height)', display: 'flex', alignItems: 'center',
          borderBottom: scrolled ? '1px solid var(--border)' : '1px solid transparent',
          transition: 'all 0.3s ease'
        }}
      >
        <div className="page-container" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none', color: 'white' }}>
            <div style={{
              width: '32px', height: '32px', borderRadius: '10px',
              background: 'linear-gradient(135deg, #FFFFFF, #A1A1AA)',
              display: 'flex', alignItems: 'center', justifyContent: 'center'
            }}>
              <Zap color="black" size={18} fill="black" />
            </div>
            <span style={{ fontSize: '18px', fontWeight: 700, letterSpacing: '-0.02em' }}>StudentGig</span>
          </Link>

          <nav style={{ display: 'flex', gap: '6px', alignItems: 'center', background: 'rgba(255,255,255,0.03)', padding: '6px', borderRadius: '99px', border: '1px solid var(--border)' }}>
            <NavLink to="/" current={location.pathname === '/'}>Overview</NavLink>
            {(!user || user.role === 'student') && <NavLink to="/jobs" current={location.pathname === '/jobs'}>Find Jobs</NavLink>}
            {(!user || user.role === 'employer') && <NavLink to="/employers" current={location.pathname === '/employers'}>For Employers</NavLink>}
          </nav>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            {user ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <button
                  onClick={() => setIsDrawerOpen(true)}
                  style={{ position: 'relative', background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                >
                  <Bell size={18} />
                  {unreadCount > 0 && (
                    <span style={{ position: 'absolute', top: 2, right: 4, background: '#EF4444', color: 'white', fontSize: '10px', fontWeight: 700, width: '16px', height: '16px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      {unreadCount}
                    </span>
                  )}
                </button>

                <div style={{ width: '1px', height: '16px', background: 'var(--border)', margin: '0 8px' }} />

                <span style={{ fontSize: '14px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Link to="/profile" style={{ color: 'inherit', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <User size={14} /> {user.name}
                  </Link>
                </span>
                <button onClick={logout} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '6px', marginLeft: '8px' }}>
                  <LogOut size={14} /> Log out
                </button>
              </div>
            ) : (
              <button onClick={() => setLoginModalOpen(true)} style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '14px', fontWeight: 500 }}>
                Log in
              </button>
            )}
            <Link to="/jobs" className="btn btn-primary" style={{ padding: '8px 16px', fontSize: '13px' }}>
              Start Earning
            </Link>
          </div>
        </div>
      </header>

      <NotificationsDrawer isOpen={isDrawerOpen} onClose={() => setIsDrawerOpen(false)} />
    </>
  );
}

// --- NOTIFICATIONS DRAWER ---
function NotificationsDrawer({ isOpen, onClose }: { isOpen: boolean, onClose: () => void }) {
  const { token } = useAuth();
  const [notifications, setNotifications] = useState<any[]>([]);

  useEffect(() => {
    if (isOpen && token) {
      // Fetch notifications when opened
      fetch(`${API_URL}/api/notifications`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
        .then(res => res.json())
        .then(data => setNotifications(data))
        .catch(() => { });

      // Mark as read in background
      fetch(`${API_URL}/api/notifications/mark-read`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      }).catch(() => { });
    }
  }, [isOpen, token]);

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            style={{ position: 'fixed', inset: 0, zIndex: 999, background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(2px)' }}
          />
          <motion.div
            initial={{ x: '100%' }}
            animate={{ x: 0 }}
            exit={{ x: '100%' }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            style={{
              position: 'fixed', top: 0, right: 0, bottom: 0, width: '400px', maxWidth: '100vw',
              background: 'var(--bg-surface-elevated)', borderLeft: '1px solid var(--border)', zIndex: 1000,
              display: 'flex', flexDirection: 'column'
            }}
          >
            <div style={{ padding: '24px', borderBottom: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h2 style={{ fontSize: '20px', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Bell size={20} /> Notifications
              </h2>
              <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '4px' }}>
                <X size={20} />
              </button>
            </div>

            <div style={{ flex: 1, overflowY: 'auto', padding: '16px' }}>
              {notifications.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-muted)' }}>
                  <Bell size={32} opacity={0.2} style={{ margin: '0 auto 12px' }} />
                  <p>You're all caught up!</p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {notifications.map((notif: any) => (
                    <div
                      key={notif.id}
                      style={{
                        padding: '16px', borderRadius: '12px',
                        background: notif.is_read ? 'rgba(255,255,255,0.02)' : 'rgba(16, 185, 129, 0.05)',
                        border: '1px solid',
                        borderColor: notif.is_read ? 'var(--border)' : 'rgba(16, 185, 129, 0.2)',
                        cursor: 'pointer'
                      }}
                    >
                      <h4 style={{ fontSize: '15px', fontWeight: 600, marginBottom: '4px', color: notif.is_read ? 'var(--text-primary)' : '#10B981' }}>{notif.title}</h4>
                      <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5 }}>{notif.message}</p>
                      <span style={{ display: 'block', fontSize: '10px', color: 'var(--text-muted)', marginTop: '8px' }}>
                        {new Date(notif.created_at).toLocaleString()}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}

function NavLink({ to, current, children }: { to: string, current: boolean, children: React.ReactNode }) {
  return (
    <Link
      to={to}
      style={{
        padding: '6px 16px', borderRadius: '99px', fontSize: '13px', fontWeight: 500,
        textDecoration: 'none', transition: 'all 0.2s ease',
        background: current ? 'rgba(255,255,255,0.1)' : 'transparent',
        color: current ? 'var(--text-primary)' : 'var(--text-secondary)'
      }}
    >
      {children}
    </Link>
  )
}

// --- LANDING PAGE ---
function LandingPage() {
  const { user } = useAuth();
  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.4 }}
    >
      <div className="hero-glow" />

      <section style={{ paddingTop: '120px', paddingBottom: '80px', textAlign: 'center', position: 'relative', zIndex: 10 }}>
        <div className="page-container" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>

          <motion.a
            href="/jobs"
            initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.1 }}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '8px',
              padding: '6px 16px 6px 6px', borderRadius: '99px',
              background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border)',
              color: 'var(--text-secondary)', fontSize: '13px', textDecoration: 'none', marginBottom: '32px'
            }}
          >
            <span className="badge badge-ai" style={{ padding: '4px 10px' }}><Sparkles size={12} /> AI-Powered</span>
            Introducing AI Skill Matching <ChevronRight size={14} />
          </motion.a>

          <motion.h1
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}
            style={{ fontSize: 'clamp(3rem, 6vw, 5.5rem)', fontWeight: 800, letterSpacing: '-0.04em', lineHeight: 1.05, maxWidth: '900px', marginBottom: '24px' }}
          >
            The intelligence layer for <br />
            <span className="text-gradient">student freelancing.</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }}
            style={{ fontSize: '20px', color: 'var(--text-secondary)', maxWidth: '600px', marginBottom: '40px' }}
          >
            StudentGig uses advanced neural matching to instantly connect your specific coursework and skills with global opportunities.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }}
            style={{ display: 'flex', gap: '16px', alignItems: 'center' }}
          >
            <Link to={user?.role === 'employer' ? "/employers" : "/jobs"} className="btn btn-primary" style={{ padding: '14px 28px', fontSize: '15px' }}>
              {user?.role === 'employer' ? "Post a Job" : "Explore Gigs"} <ArrowRight size={16} />
            </Link>
            <button onClick={() => window.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', metaKey: true }))} className="mono" style={{ color: 'var(--text-muted)', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', background: 'transparent', border: 'none' }}>
              <Command size={14} /> K
            </button>
          </motion.div>
        </div>
      </section>

      {/* AI Bento Grid */}
      <section style={{ padding: '80px 0', position: 'relative', zIndex: 10 }}>
        <div className="page-container">
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px', gridAutoRows: 'minmax(250px, auto)' }}>

            {/* Bento 1: Large Feature */}
            <BentoCard colSpan={2} delay={0.5}>
              <div style={{ padding: '40px', display: 'flex', flexDirection: 'column', height: '100%' }}>
                <BrainCircuit color="var(--primary)" size={32} style={{ marginBottom: '24px' }} />
                <h3 style={{ fontSize: '24px', fontWeight: 600, letterSpacing: '-0.02em', marginBottom: '12px' }}>Hyper-Personalized Feed</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '16px', maxWidth: '400px' }}>
                  Our AI analyzes your skills profile and past gig history to mathematically rank the best-paying projects for you.
                </p>
                <div style={{ flex: 1 }} />
                <div style={{ display: 'flex', gap: '8px', marginTop: '32px' }}>
                  <span className="badge badge-ai">98% Match Rate</span>
                  <span className="badge badge-neutral">Auto-Apply</span>
                </div>
              </div>
            </BentoCard>

            {/* Bento 2: Stats */}
            <BentoCard colSpan={1} delay={0.6}>
              <div style={{ padding: '40px', display: 'flex', flexDirection: 'column', height: '100%', justifyContent: 'center', alignItems: 'center', textAlign: 'center' }}>
                <div style={{ fontSize: '48px', fontWeight: 800, color: 'var(--text-primary)', letterSpacing: '-0.04em' }}>₹1.2M+</div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '15px' }}>Earned by students this month</p>
              </div>
            </BentoCard>

            {/* Bento 3: Tech */}
            <BentoCard colSpan={1} delay={0.7}>
              <div style={{ padding: '40px', display: 'flex', flexDirection: 'column' }}>
                <Code color="var(--accent)" size={32} style={{ marginBottom: '24px' }} />
                <h3 style={{ fontSize: '20px', fontWeight: 600, letterSpacing: '-0.02em', marginBottom: '8px' }}>Cross-Platform Ready</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>Works seamlessly across web, Android, and iOS — powered by modern cloud APIs.</p>
              </div>
            </BentoCard>

            {/* Bento 4: Speed */}
            <BentoCard colSpan={2} delay={0.8}>
              <div style={{ padding: '40px', display: 'flex', flexDirection: 'column', height: '100%', position: 'relative', overflow: 'hidden' }}>
                <Target color="var(--success)" size={32} style={{ marginBottom: '24px' }} />
                <h3 style={{ fontSize: '24px', fontWeight: 600, letterSpacing: '-0.02em', marginBottom: '12px' }}>Get hired in milliseconds.</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '16px', maxWidth: '400px' }}>
                  Real-time WebSockets connect you to employers exactly when they hit "Post". No waiting.
                </p>
                <div style={{ position: 'absolute', right: '-20px', bottom: '-20px', opacity: 0.1 }}>
                  <Zap size={200} />
                </div>
              </div>
            </BentoCard>

          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section style={{ padding: '80px 0', position: 'relative', zIndex: 10 }}>
        <div className="page-container">
          <div style={{ textAlign: 'center', marginBottom: '48px' }}>
            <motion.h2
              initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}
              style={{ fontSize: '36px', fontWeight: 800, letterSpacing: '-0.03em', marginBottom: '12px' }}
            >
              How it <span className="text-gradient-primary">works</span>
            </motion.h2>
            <motion.p
              initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ delay: 0.1 }}
              style={{ color: 'var(--text-secondary)', fontSize: '17px', maxWidth: '500px', margin: '0 auto' }}
            >
              From sign-up to first paycheck in under 10 minutes.
            </motion.p>
          </div>

          <div className="how-it-works-grid">
            {[
              { icon: <UserPlus size={24} />, title: 'Create Profile', desc: 'Sign up with your phone or Google and add your skills, coursework, and availability.' },
              { icon: <BrainCircuit size={24} />, title: 'AI Matches You', desc: 'Our neural engine ranks every new gig against your unique skill profile in real-time.' },
              { icon: <Briefcase size={24} />, title: 'Apply & Work', desc: 'One-tap apply. Show up, get verified, and complete the gig on your schedule.' },
              { icon: <Zap size={24} />, title: 'Get Paid Instantly', desc: 'Confirm completion, receive payment, and build your verified reputation score.' },
            ].map((step, i) => (
              <motion.div
                key={i}
                className="how-step"
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
              >
                <div className="how-step-number">{i + 1}</div>
                <div style={{ color: 'var(--primary)', marginBottom: '16px', display: 'flex', justifyContent: 'center' }}>
                  {step.icon}
                </div>
                <h4>{step.title}</h4>
                <p>{step.desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats / Social Proof */}
      <section style={{ padding: '60px 0', position: 'relative', zIndex: 10 }}>
        <div className="page-container">
          <motion.div
            initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}
            style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px', textAlign: 'center', padding: '48px 0', borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)' }}
          >
            <div>
              <div style={{ fontSize: '42px', fontWeight: 800, letterSpacing: '-0.04em' }} className="text-gradient">2,500+</div>
              <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginTop: '4px' }}>Students earning</p>
            </div>
            <div>
              <div style={{ fontSize: '42px', fontWeight: 800, letterSpacing: '-0.04em' }} className="text-gradient">98%</div>
              <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginTop: '4px' }}>AI match accuracy</p>
            </div>
            <div>
              <div style={{ fontSize: '42px', fontWeight: 800, letterSpacing: '-0.04em' }} className="text-gradient">&lt;30s</div>
              <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginTop: '4px' }}>Average match time</p>
            </div>
          </motion.div>
        </div>
      </section>
    </motion.div>
  )
}

function BentoCard({ children, colSpan = 1, delay = 0 }: any) {
  const cardRef = useRef<HTMLDivElement>(null);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    cardRef.current.style.setProperty('--mouse-x', `${e.clientX - rect.left}px`);
    cardRef.current.style.setProperty('--mouse-y', `${e.clientY - rect.top}px`);
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ duration: 0.5, delay }}
      ref={cardRef}
      onMouseMove={handleMouseMove}
      className="spotlight-card"
      style={{ gridColumn: `span ${colSpan}` }}
    >
      <div className="spotlight-content" style={{ height: '100%' }}>
        {children}
      </div>
    </motion.div>
  )
}

// --- FUNCTIONAL JOBS PAGE ---
function JobsPage() {
  const { token, user, setLoginModalOpen } = useAuth();

  // Role Protection: Employers should not see the student jobs feed.
  if (user?.role === 'employer') {
    return <Navigate to="/employers" />;
  }

  const { toast } = useToast();
  const [search, setSearch] = useState('');
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [jobs, setJobs] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedJob, setSelectedJob] = useState<any>(null);
  const [isApplying, setIsApplying] = useState(false);

  // AI Search states
  const [isAiSearch, setIsAiSearch] = useState(false);
  const [aiInterpretation, setAiInterpretation] = useState<string | null>(null);
  const [isAiLoading, setIsAiLoading] = useState(false);

  // Match Explanation states
  const [matchDetail, setMatchDetail] = useState<any>(null);
  const [isMatchLoading, setIsMatchLoading] = useState(false);

  // Fallback data in case the backend server isn't actively running
  const dummyJobs = [
    { id: 1, title: 'Python Web Scraper & API', company: 'DataSync Ltd', location: 'Remote', description: 'Build a production-ready web scraper using Python and FastAPI. Must handle pagination, rate limiting, and export to CSV/JSON.', tags: ['Python', 'FastAPI', 'BS4'], category: 'Development', match: 96, price: '₹12,000', urgent: true, created_at: new Date(Date.now() - 3600000).toISOString(), duration: '1 week' },
    { id: 2, title: 'React Frontend Refactor', company: 'Stealth Startup', location: 'Bangalore', description: 'Refactor an existing React app to use TypeScript and modern hooks. Improve component structure and add proper error handling.', tags: ['React', 'TypeScript', 'CSS'], category: 'Development', match: 88, price: '₹25,000', urgent: false, created_at: new Date(Date.now() - 86400000).toISOString(), duration: '2 weeks' },
    { id: 3, title: 'Figma UI/UX Prototype', company: 'NextGen Design', location: 'Remote', description: 'Design a mobile-first UI prototype for a food delivery app. Create wireframes, high-fidelity mockups, and interactive prototype.', tags: ['Figma', 'Prototyping'], category: 'Design', match: 92, price: '₹8,500', urgent: true, created_at: new Date(Date.now() - 7200000).toISOString(), duration: '3 days' },
    { id: 4, title: 'Data Entry Database Cleanup', company: 'TechCorp', location: 'Hyderabad', description: 'Clean up and organize a large Excel dataset with 5000+ records. Remove duplicates, fix formatting, and import into SQL database.', tags: ['Excel', 'Data Entry', 'SQL'], category: 'Data Entry', match: 82, price: '₹15,000', urgent: false, created_at: new Date(Date.now() - 172800000).toISOString(), duration: '5 days' },
    { id: 5, title: 'Android Navigation Fix', company: 'AppWorks', location: 'Remote', description: 'Fix broken navigation flow in an Android Kotlin app using Jetpack Compose. Restore back-stack behavior and deep linking.', tags: ['Kotlin', 'Compose', 'Android'], category: 'Development', match: 99, price: '₹5,000', urgent: true, created_at: new Date(Date.now() - 1800000).toISOString(), duration: '2 hours' },
  ];

  useEffect(() => {
    fetch(`${API_URL}/api/jobs`, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    })
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data) && data.length > 0) {
          // Map backend schema to UI schema smoothly
          setJobs(data.map(d => ({
            id: d.id, title: d.title, company: d.employer_name || d.company_name || 'Unknown',
            location: d.location || 'Not specified',
            description: d.description || '',
            tags: d.skills_required ? JSON.parse(d.skills_required) : [],
            category: d.category || 'Development',
            match: d.match_score || Math.floor(Math.random() * 20) + 80,
            price: `₹${d.pay_amount}`, urgent: d.is_urgent,
            created_at: d.created_at || null,
            duration: d.duration || null,
          })));
        } else {
          setJobs(dummyJobs);
        }
      })
      .catch(() => {
        console.warn('Backend disconnected. Using UI mock data.');
        setJobs(dummyJobs);
      })
      .finally(() => setIsLoading(false));
  }, [token]);

  const handleSearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!search || !isAiSearch) return;

    setIsAiLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/ai/smart-search`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({ query: search })
      });
      const data = await res.json();
      if (res.ok && data.jobs) {
        setAiInterpretation(data.interpretation);
        setJobs(data.jobs.map((d: any) => ({
          id: d.id, title: d.title, company: d.employer_name || d.company_name || 'Unknown',
          location: d.location || 'Not specified',
          description: d.description || '',
          tags: d.skills_required ? JSON.parse(d.skills_required) : [],
          category: d.category || 'Development',
          match: d.match_score || 0,
          price: `₹${d.pay_amount}`, urgent: d.is_urgent,
          created_at: d.created_at || null,
          duration: d.duration || null,
        })));
      } else {
        toast('Smart Search encountered an error.', 'warning');
      }
    } catch (err) {
      toast('Failed to reach AI engine.', 'error');
    } finally {
      setIsAiLoading(false);
    }
  };

  const fetchMatchExplanation = async (jobId: number) => {
    if (!token) {
      toast('Please log in to see match analysis', 'info');
      return;
    }
    setIsMatchLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/ai/match-explanation/${jobId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        setMatchDetail(await res.json());
      }
    } catch (err) {
      toast('Could not analyze match.', 'error');
    } finally {
      setIsMatchLoading(false);
    }
  };

  const filteredJobs = jobs.filter(j =>
    !isAiSearch ? (
      (j.title.toLowerCase().includes(search.toLowerCase()) || j.tags.some((t: string) => t.toLowerCase().includes(search.toLowerCase()))) &&
      (activeCategory ? j.category === activeCategory : true)
    ) : true
  );

  const handleApply = async () => {
    if (!token || !user) {
      setLoginModalOpen(true);
      return;
    }
    if (user.role === 'employer') {
      toast('Employers cannot apply for gigs. Please use a student account to work.', 'warning');
      return;
    }
    setIsApplying(true);
    try {
      const res = await fetch(`${API_URL}/api/apply`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ job_id: selectedJob.id })
      });
      const data = await res.json();
      if (res.ok) {
        toast('Applied successfully! Track it in your profile.', 'success');
        setSelectedJob(null);
      } else {
        toast(data.detail || 'Failed to apply.', 'error');
      }
    } catch (err) {
      toast('Error applying to job. Please try again.', 'error');
    } finally {
      setIsApplying(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      transition={{ duration: 0.4 }}
      style={{ padding: '40px 0', minHeight: '80vh' }}
    >
      <div className="page-container">

        {/* Search Header */}
        <div style={{ marginBottom: '40px' }}>
          <h1 style={{ fontSize: '32px', fontWeight: 700, letterSpacing: '-0.02em', marginBottom: '8px' }}>Explore Gigs</h1>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>Discover projects mathematically ranked for your skill set.</p>

          <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
            <form onSubmit={handleSearch} style={{ flex: 1, display: 'flex' }}>
              <div className="search-bar-large">
                <Search size={20} color={isAiSearch ? "var(--primary)" : "var(--text-muted)"} />
                <input
                  type="text"
                  placeholder={isAiSearch ? "Try 'weekend photography in Hyderabad'..." : "Search Python, React, Data Entry..."}
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />

                {search && isAiSearch && (
                  <button type="submit" className="badge badge-ai" style={{ border: 'none', cursor: 'pointer', padding: '6px 12px' }}>
                    {isAiLoading ? 'Analyzing...' : 'Search'}
                  </button>
                )}

                {!search && (
                  <button
                    type="button"
                    onClick={() => window.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', metaKey: true }))}
                    className="badge badge-neutral mono"
                    style={{ padding: '4px 8px', fontSize: '10px', cursor: 'pointer', background: 'transparent' }}>
                    Cmd K
                  </button>
                )}
              </div>
            </form>
            <button
              onClick={() => {
                setIsAiSearch(!isAiSearch);
                if (!isAiSearch) {
                  setSearch('');
                  setAiInterpretation(null);
                }
              }}
              className={`btn ${isAiSearch ? 'btn-glow badge-ai-live' : 'badge-neutral'}`}
              style={{ padding: '8px 16px', fontSize: '14px', borderRadius: '12px', border: isAiSearch ? '1px solid var(--primary)' : '1px solid var(--border)', transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)' }}
            >
              <Sparkles size={16} color={isAiSearch ? "var(--primary)" : "var(--text-muted)"} style={{ marginRight: '8px' }} />
              {isAiSearch ? 'Smart Mode On' : 'Smart Search'}
            </button>
          </div>

          {isAiSearch && (
            <motion.div
              initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }}
              style={{ marginTop: '16px', padding: '12px 20px', background: 'rgba(139, 92, 246, 0.05)', borderRadius: '12px', border: '1px solid rgba(139, 92, 246, 0.15)', display: 'flex', alignItems: 'center', gap: '10px' }}
            >
              <Sparkles size={14} color="var(--primary)" className={isAiLoading ? "animate-spin" : ""} />
              <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                {isAiLoading ? (
                  <span style={{ opacity: 0.6 }}>AI Engine interpreting your intent...</span>
                ) : aiInterpretation ? (
                  <>
                    <span style={{ fontWeight: 600, color: 'var(--primary)' }}>AI Interpretation:</span> {aiInterpretation}
                  </>
                ) : (
                  <span style={{ opacity: 0.6 }}>Tell me what you're looking for (e.g., "delivery jobs near me")</span>
                )}
              </p>
            </motion.div>
          )}
        </div>

        {/* Jobs Grid / List */}
        <div style={{ display: 'flex', gap: '32px' }}>
          {/* Main Feed */}
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {isLoading ? (
              <div style={{ textAlign: 'center', padding: '60px' }}>
                <div className="animate-float" style={{ fontSize: '24px', color: 'var(--primary)' }}>Loading intelligently...</div>
              </div>
            ) : filteredJobs.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '60px', border: '1px dashed var(--border)', borderRadius: '16px' }}>
                <Search size={40} color="var(--text-muted)" style={{ marginBottom: '16px' }} />
                <h3 style={{ fontSize: '18px', fontWeight: 600, color: 'var(--text-primary)' }}>No exact matches found</h3>
                <p style={{ color: 'var(--text-muted)' }}>We couldn't find anything matching your filters.</p>
              </div>
            ) : (
              <AnimatePresence>
                {filteredJobs.map((job, idx) => (
                  <JobListItem 
                    key={job.id} 
                    job={job} 
                    index={idx} 
                    onClick={() => setSelectedJob(job)} 
                    onMatchClick={fetchMatchExplanation}
                  />
                ))}
              </AnimatePresence>
            )}
          </div>

          {/* Sidebar / Filters (Desktop) */}
          <div className="filters-sidebar" style={{ width: '320px' }}>
            <div className="sidebar-card">
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '24px' }}>
                <Filter size={18} color="var(--primary)" />
                <h3 style={{ fontSize: '16px', fontWeight: 700 }}>Refine Search</h3>
              </div>

              <div style={{ marginBottom: '32px' }}>
                <p style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: '16px' }}>Category</p>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                  {['Development', 'Design', 'Data Entry', 'Writing', 'Photography'].map(cat => (
                    <label key={cat} style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '14px', color: activeCategory === cat ? 'var(--text-primary)' : 'var(--text-secondary)', cursor: 'pointer', transition: 'color 0.2s' }}>
                      <input
                        type="checkbox"
                        checked={activeCategory === cat}
                        onChange={() => setActiveCategory(activeCategory === cat ? null : cat)}
                        style={{ width: '18px', height: '18px', borderRadius: '4px', accentColor: 'var(--primary)' }}
                      /> {cat}
                    </label>
                  ))}
                </div>
              </div>

              <div className="divider-ai" style={{ margin: '24px 0' }} />

              <div>
                <p style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: '16px' }}>Quick Stats</p>
                <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span>Live Gig Matches</span>
                    <span style={{ fontWeight: 700, color: 'var(--primary)' }}>{filteredJobs.length}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span>Search Mode</span>
                    <span style={{ fontWeight: 600 }}>{isAiSearch ? 'AI Neural' : 'Manual'}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Apply Modal */}
      <AnimatePresence>
        {selectedJob && (
          <div style={{ position: 'fixed', inset: 0, zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div
              onClick={() => setSelectedJob(null)}
              style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}
            />
            <motion.div
              initial={{ scale: 0.95, y: -20, opacity: 0 }}
              animate={{ scale: 1, y: 0, opacity: 1 }}
              exit={{ scale: 0.95, y: -20, opacity: 0 }}
              className="spotlight-card"
              style={{ position: 'relative', width: '90%', maxWidth: '560px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border)', borderRadius: '24px', padding: '32px', maxHeight: '85vh', overflowY: 'auto' }}
            >
              {/* Close button */}
              <button onClick={() => setSelectedJob(null)} style={{ position: 'absolute', top: '16px', right: '16px', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border)', borderRadius: '8px', padding: '6px', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex' }}>
                <X size={16} />
              </button>

              {/* Header */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
                <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                  <Briefcase color="var(--primary)" size={24} />
                </div>
                <div>
                  <h2 style={{ fontSize: '22px', fontWeight: 700, letterSpacing: '-0.02em' }}>{selectedJob.title}</h2>
                  <p style={{ color: 'var(--text-muted)', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px', marginTop: '4px' }}>
                    <User size={12} /> Posted by {selectedJob.company}
                    {selectedJob.created_at && <><span style={{ opacity: 0.3 }}>•</span> <Clock size={12} /> {timeAgo(selectedJob.created_at)}</>}
                  </p>
                </div>
              </div>

              {/* Info Grid */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '20px' }}>
                <div style={{ background: 'rgba(0,0,0,0.3)', padding: '14px', borderRadius: '12px', border: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-muted)', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Budget</span>
                  <p style={{ fontWeight: 700, fontSize: '18px', marginTop: '4px' }}>{selectedJob.price}</p>
                </div>
                <div style={{ background: 'rgba(0,0,0,0.3)', padding: '14px', borderRadius: '12px', border: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-muted)', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>AI Match</span>
                  <p
                    className="text-gradient-ai"
                    style={{ fontWeight: 700, fontSize: '18px', marginTop: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
                    onClick={() => fetchMatchExplanation(selectedJob.id)}
                  >
                    {selectedJob.match}% {isMatchLoading ? '...' : <Sparkles size={16} />}
                  </p>
                </div>
                <div style={{ background: 'rgba(0,0,0,0.3)', padding: '14px', borderRadius: '12px', border: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-muted)', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', display: 'flex', alignItems: 'center', gap: '4px' }}><MapPin size={10} /> Location</span>
                  <p style={{ fontWeight: 600, fontSize: '14px', marginTop: '4px' }}>{selectedJob.location}</p>
                </div>
                <div style={{ background: 'rgba(0,0,0,0.3)', padding: '14px', borderRadius: '12px', border: '1px solid var(--border)' }}>
                  <span style={{ color: 'var(--text-muted)', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.04em', display: 'flex', alignItems: 'center', gap: '4px' }}><Clock size={10} /> Duration</span>
                  <p style={{ fontWeight: 600, fontSize: '14px', marginTop: '4px' }}>{selectedJob.duration || 'Flexible'}</p>
                </div>
              </div>

              {/* Description */}
              {selectedJob.description && (
                <div style={{ marginBottom: '20px' }}>
                  <h4 style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '8px' }}>About this gig</h4>
                  <p style={{ fontSize: '14px', color: 'var(--text-secondary)', lineHeight: 1.7, background: 'rgba(0,0,0,0.2)', padding: '16px', borderRadius: '12px', border: '1px solid var(--border)' }}>
                    {selectedJob.description}
                  </p>
                </div>
              )}

              {/* Skills Tags */}
              {selectedJob.tags && selectedJob.tags.length > 0 && (
                <div style={{ marginBottom: '20px' }}>
                  <h4 style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '8px' }}>Skills Required</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                    {selectedJob.tags.map((tag: string) => (
                      <span key={tag} className="badge badge-neutral" style={{ padding: '6px 12px', fontSize: '12px' }}>{tag}</span>
                    ))}
                  </div>
                </div>
              )}

              {/* Category + Urgent badges */}
              <div style={{ display: 'flex', gap: '8px', marginBottom: '24px', flexWrap: 'wrap' }}>
                {selectedJob.category && <span className="badge badge-neutral" style={{ padding: '6px 12px', fontSize: '12px' }}>{selectedJob.category}</span>}
                {selectedJob.urgent && <span className="badge" style={{ background: 'rgba(245, 158, 11, 0.1)', color: '#F59E0B', border: '1px solid rgba(245, 158, 11, 0.2)', padding: '6px 12px', fontSize: '12px' }}>🔥 Urgent</span>}
              </div>

              <button
                onClick={handleApply}
                disabled={isApplying}
                className="btn btn-primary"
                style={{ width: '100%', padding: '16px', fontSize: '16px', opacity: isApplying ? 0.7 : 1 }}
              >
                {isApplying ? 'Applying...' : 'Apply with AI Matching'}
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* AI Match Explanation Modal */}
      <AnimatePresence>
        {matchDetail && (
          <div style={{ position: 'fixed', inset: 0, zIndex: 1100, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div
              onClick={() => setMatchDetail(null)}
              style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.7)', backdropFilter: 'blur(8px)' }}
            />
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.9, opacity: 0 }}
              className="spotlight-card"
              style={{ position: 'relative', width: '90%', maxWidth: '500px', background: 'var(--bg-surface-elevated)', padding: '32px', borderRadius: '24px', border: '1px solid var(--border-glow)' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
                <div style={{ width: '40px', height: '40px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <Sparkles color="#10B981" />
                </div>
                <div>
                  <h3 style={{ fontSize: '20px', fontWeight: 700 }}>AI Match Analysis</h3>
                  <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>How you fit for this role</p>
                </div>
              </div>

              <div style={{ marginBottom: '24px' }}>
                <h4 style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '12px' }}>Matched Skills</h4>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {matchDetail.matched_skills.map((s: string) => (
                    <span key={s} className="badge badge-ai" style={{ padding: '6px 12px' }}>{s}</span>
                  ))}
                  {matchDetail.matched_skills.length === 0 && <p style={{ fontSize: '13px', color: 'var(--text-muted)' }}>No skills matched yet. Try adding more to your profile!</p>}
                </div>
              </div>

              {matchDetail.missing_skills.length > 0 && (
                <div style={{ marginBottom: '24px' }}>
                  <h4 style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '12px' }}>Recommended to Add</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                    {matchDetail.missing_skills.map((s: string) => (
                      <span key={s} className="badge badge-neutral" style={{ padding: '6px 12px' }}>{s}</span>
                    ))}
                  </div>
                </div>
              )}

              <div style={{ background: 'rgba(255,255,255,0.03)', padding: '16px', borderRadius: '16px', border: '1px solid var(--border)' }}>
                <p style={{ fontSize: '14px', lineHeight: 1.6, color: 'var(--text-secondary)' }}>
                  <span style={{ fontWeight: 700, color: 'var(--primary)' }}>AI Insight:</span> {matchDetail.explanation}
                </p>
              </div>

              <button onClick={() => setMatchDetail(null)} className="btn btn-primary" style={{ width: '100%', marginTop: '24px', padding: '14px' }}>
                Got it
              </button>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </motion.div>
  )
}

function JobListItem({ job, index, onClick, onMatchClick }: any) {
  const cardRef = useRef<HTMLDivElement>(null);
  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    cardRef.current.style.setProperty('--mouse-x', `${e.clientX - rect.left}px`);
    cardRef.current.style.setProperty('--mouse-y', `${e.clientY - rect.top}px`);
  };

  // Truncate description to ~120 chars
  const shortDesc = job.description
    ? (job.description.length > 120 ? job.description.slice(0, 120) + '...' : job.description)
    : null;

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95 }}
      transition={{ delay: index * 0.05 }}
      ref={cardRef} onMouseMove={handleMouseMove} onClick={onClick}
      className="spotlight-card"
      style={{ padding: '24px', cursor: 'pointer', display: 'flex', gap: '20px' }}
    >
      {/* Icon Area */}
      <div className="spotlight-content" style={{ display: 'flex', alignItems: 'flex-start' }}>
        <div style={{ width: '48px', height: '48px', borderRadius: '12px', background: 'rgba(139, 92, 246, 0.05)', border: '1px solid var(--border-glow)', display: 'flex', alignItems: 'center', justifyContent: 'center', transition: 'all 0.3s ease' }} className="icon-container">
          <Briefcase color="var(--primary)" size={20} />
        </div>
      </div>

      {/* Main Content Info */}
      <div className="spotlight-content" style={{ flex: 1 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <h3 style={{ fontSize: '18px', fontWeight: 600, letterSpacing: '-0.01em', marginBottom: '4px' }}>{job.title}</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
              <User size={12} style={{ opacity: 0.6 }} /> Posted by {job.company}
              <span style={{ opacity: 0.3 }}>•</span> <MapPin size={12} style={{ opacity: 0.6 }} /> {job.location || 'Not specified'}
              {job.created_at && <><span style={{ opacity: 0.3 }}>•</span> <Clock size={11} style={{ opacity: 0.5 }} /> <span style={{ opacity: 0.7 }}>{timeAgo(job.created_at)}</span></>}
            </p>
          </div>
          <div style={{ textAlign: 'right', flexShrink: 0, marginLeft: '16px' }}>
            <span style={{ fontSize: '18px', fontWeight: 700, color: 'var(--text-primary)' }}>{job.price}</span>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Fixed Budget</p>
          </div>
        </div>

        {/* Description Preview */}
        {shortDesc && (
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.5, margin: '8px 0 12px', opacity: 0.8 }}>
            {shortDesc}
          </p>
        )}

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
          <span
            className={`badge badge-ai ${job.match > 90 ? 'badge-ai-live' : ''}`}
            style={{ cursor: 'pointer' }}
            onClick={(e) => {
              e.stopPropagation();
              onClick(); // Open job modal first
              setTimeout(() => onMatchClick(job.id), 100); // Small delay to let modal open
            }}
          >
            <Sparkles size={12} /> {job.match}% Match
          </span>
          {job.urgent && <span className="badge" style={{ background: 'rgba(245, 158, 11, 0.1)', color: '#F59E0B', border: '1px solid rgba(245, 158, 11, 0.2)' }}>🔥 Urgent</span>}
          {job.duration && <span className="badge badge-neutral" style={{ padding: '2px 8px', fontSize: '11px' }}><Clock size={10} style={{ marginRight: '4px' }} />{job.duration}</span>}

          <div style={{ flex: 1 }} />

          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            {job.tags.slice(0, 4).map((tag: string) => (
              <span key={tag} className="badge badge-neutral" style={{ padding: '2px 8px', fontSize: '11px', fontWeight: 500 }}>{tag}</span>
            ))}
            {job.tags.length > 4 && <span className="badge badge-neutral" style={{ padding: '2px 8px', fontSize: '11px' }}>+{job.tags.length - 4}</span>}
          </div>
        </div>
      </div>
    </motion.div>
  )
}

// --- EMPLOYERS PAGE ---
const JOB_CATEGORIES = [
  "Tutoring", "Delivery", "Events", "Tech", "Content Creation",
  "Design", "Marketing", "Data Entry", "Photography", "Volunteering",
  "Writing", "Translation", "Hospitality", "Fitness", "Other"
];

function EmployersPage() {
  const { user, token, setLoginModalOpen } = useAuth();
  const { toast } = useToast();
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [budget, setBudget] = useState('');
  const [category, setCategory] = useState('');
  const [location, setLocation] = useState('');
  const [skillInput, setSkillInput] = useState('');
  const [skills, setSkills] = useState<string[]>([]);
  const [isUrgent, setIsUrgent] = useState(false);
  const [duration, setDuration] = useState('');
  const [loading, setLoading] = useState(false);
  const [postSuccess, setPostSuccess] = useState(false);

  // AI Assistant states
  const [isGeneratingDesc, setIsGeneratingDesc] = useState(false);
  const [isEstimatingPay, setIsEstimatingPay] = useState(false);

  const [employerApps, setEmployerApps] = useState<any[]>([]);

  const refreshEmployerApps = useCallback(() => {
    if (!token) return;
    fetch(`${API_URL}/api/employer/applications`, {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => res.json())
      .then(data => setEmployerApps(Array.isArray(data) ? data : []))
      .catch(() => { });
  }, [token]);

  useEffect(() => {
    refreshEmployerApps();
  }, [refreshEmployerApps]);

  const addSkill = () => {
    const trimmed = skillInput.trim().toLowerCase();
    if (trimmed && !skills.includes(trimmed)) {
      setSkills([...skills, trimmed]);
    }
    setSkillInput('');
  };

  const removeSkill = (skillToRemove: string) => {
    setSkills(skills.filter(s => s !== skillToRemove));
  };

  const handleSkillKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addSkill();
    }
  };

  const generateAiDescription = async () => {
    if (!title) {
      toast('Please enter a title first.', 'warning');
      return;
    }
    setIsGeneratingDesc(true);
    try {
      const res = await fetch(`${API_URL}/api/ai/generate-description`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, category, location, duration, rough_notes: desc })
      });
      const data = await res.json();
      if (res.ok) {
        setDesc(data.description);
        if (data.suggested_skills) {
          try {
            const suggested = typeof data.suggested_skills === 'string' ? JSON.parse(data.suggested_skills) : data.suggested_skills;
            if (Array.isArray(suggested)) {
              setSkills(prev => Array.from(new Set([...prev, ...suggested])));
            }
          } catch (_) { /* ignore parse errors */ }
        }
        toast('AI Polished your description!', 'success');
      }
    } catch (err) {
      toast('AI generation failed.', 'error');
    } finally {
      setIsGeneratingDesc(false);
    }
  };

  const estimateAiPay = async () => {
    if (!category || !location) {
      toast('Please select category and location first.', 'warning');
      return;
    }
    setIsEstimatingPay(true);
    try {
      const res = await fetch(`${API_URL}/api/ai/estimate-pay`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ category, location, duration, job_type: 'one-time' })
      });
      const data = await res.json();
      if (res.ok && data.avg_pay != null) {
        setBudget(Math.round(data.avg_pay).toString());
        toast(`AI suggests ₹${Math.round(data.avg_pay)} based on ${data.sample_size} similar gigs (${data.confidence} confidence)`, 'info');
      }
    } catch (err) {
      toast('Market analysis failed.', 'error');
    } finally {
      setIsEstimatingPay(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !token) {
      setLoginModalOpen(true);
      return;
    }
    if (user.role === 'student') {
      toast('Only employers can post jobs. Please use an employer account.', 'warning');
      return;
    }
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/jobs`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          title,
          description: desc,
          pay_amount: parseFloat(budget) || 0,
          category: category || 'Other',
          skills_required: JSON.stringify(skills),
          is_urgent: isUrgent,
          location: location || 'Not specified',
          duration: duration || null,
        })
      });
      if (res.ok) {
        setPostSuccess(true);
        setTitle(''); setDesc(''); setBudget(''); setCategory(''); setLocation('');
        setSkills([]); setIsUrgent(false); setDuration('');
        setTimeout(() => {
          setPostSuccess(false);
          window.location.href = '/jobs';
        }, 2000);
      } else {
        toast('Failed to post job. Please check all fields.', 'error');
      }
    } catch (err) {
      toast('Error posting job. Check your connection.', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
      style={{ padding: '60px 0', minHeight: '80vh' }}
    >
      <div className="page-container" style={{ maxWidth: '900px' }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '48px' }}>
          <div style={{ width: '64px', height: '64px', borderRadius: '16px', background: 'rgba(139, 92, 246, 0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', border: '1px solid var(--border-glow)' }}>
            <Target color="var(--primary)" size={32} />
          </div>
          <h1 style={{ fontSize: '40px', fontWeight: 800, letterSpacing: '-0.03em', marginBottom: '16px' }}>
            Hire <span className="text-gradient-primary">Top 1%</span> Student Talent
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '18px', maxWidth: '600px', margin: '0 auto' }}>
            Post your project and let our AI engine match you with the best students. Fill in the details below to get started.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: employerApps.length > 0 ? '1.2fr 0.8fr' : '1fr', gap: '32px' }}>
          {/* Post Job Form */}
          <form onSubmit={handleSubmit} className="spotlight-card" style={{ padding: '32px' }}>
            {/* Success Banner */}
            {postSuccess && (
              <motion.div
                initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}
                style={{ background: 'rgba(16, 185, 129, 0.1)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: '12px', padding: '16px', marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '12px', color: '#10B981' }}
              >
                <CheckCircle2 size={20} />
                <span style={{ fontWeight: 600 }}>Job posted successfully! Redirecting to feed...</span>
              </motion.div>
            )}

            <h3 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '24px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Briefcase size={20} /> Post a New Gig
            </h3>
            {/* --- Section 1: Basics --- */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
              <div style={{ padding: '6px', background: 'rgba(139, 92, 246, 0.1)', borderRadius: '8px' }}><Info size={16} color="var(--primary)" /></div>
              <h4 style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-primary)' }}>Overview</h4>
            </div>

            <div className="input-group">
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Project Title</label>
              <input className="input-field" value={title} onChange={e => setTitle(e.target.value)} required placeholder="e.g. Quick Python Fix, Web Scraping Task" />
            </div>

            <div className="input-group">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <label style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Role Description</label>
                <button
                  type="button"
                  onClick={generateAiDescription}
                  disabled={isGeneratingDesc}
                  className={`badge badge-ai ${isGeneratingDesc ? 'badge-ai-live' : ''}`}
                  style={{ border: 'none', cursor: 'pointer', padding: '4px 10px', fontSize: '11px' }}
                >
                  <Sparkles size={12} className={isGeneratingDesc ? "animate-spin" : ""} /> {isGeneratingDesc ? 'Enriching...' : 'Polish with AI'}
                </button>
              </div>
              <textarea className="input-field" value={desc} onChange={e => setDesc(e.target.value)} required placeholder="Briefly describe the task. Our AI will help you expand this into a professional post." style={{ minHeight: '100px', resize: 'vertical' }}></textarea>
            </div>

            <div className="divider-ai" />

            {/* --- Section 2: Details --- */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
              <div style={{ padding: '6px', background: 'rgba(56, 189, 248, 0.1)', borderRadius: '8px' }}><Target size={16} color="var(--accent)" /></div>
              <h4 style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-primary)' }}>Logistics</h4>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px' }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                  <label style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Est. Budget (₹) *</label>
                  <button type="button" onClick={estimateAiPay} disabled={isEstimatingPay} className="text-gradient-ai" style={{ background: 'none', border: 'none', fontSize: '10px', fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Target size={10} /> {isEstimatingPay ? '...' : 'AI Price Estimate'}
                  </button>
                </div>
                <input className="input-field" type="number" min="1" value={budget} onChange={e => setBudget(e.target.value)} required placeholder="500" />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Primary Category</label>
                <select className="input-field" value={category} onChange={e => setCategory(e.target.value)} required style={{ appearance: 'none', background: 'var(--bg-surface)', border: '1px solid var(--border)', color: 'white', padding: '14px 18px', borderRadius: '12px', width: '100%', outline: 'none', cursor: 'pointer' }}>
                  <option value="" disabled>Select</option>
                  {JOB_CATEGORIES.map(cat => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '24px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Location</label>
                <div style={{ position: 'relative' }}>
                  <MapPin size={14} style={{ position: 'absolute', left: '14px', top: '16px', color: 'var(--text-muted)' }} />
                  <input className="input-field" style={{ paddingLeft: '40px' }} value={location} onChange={e => setLocation(e.target.value)} required placeholder="e.g. Bangalore, Remote" />
                </div>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>Duration</label>
                <input className="input-field" value={duration} onChange={e => setDuration(e.target.value)} placeholder="e.g. 2 hours, 1 week" />
              </div>
            </div>

            <div className="divider-ai" />

            {/* --- Section 3: Skills --- */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
              <div style={{ padding: '6px', background: 'rgba(16, 185, 129, 0.1)', borderRadius: '8px' }}><Code size={16} color="var(--success)" /></div>
              <h4 style={{ fontSize: '14px', fontWeight: 700, color: 'var(--text-primary)' }}>Desired Skills</h4>
            </div>

            <div style={{ marginBottom: '24px' }}>
              <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
                <input
                  className="input-field"
                  value={skillInput}
                  onChange={e => setSkillInput(e.target.value)}
                  onKeyDown={handleSkillKeyDown}
                  placeholder="e.g. React, Python, Hindi (Press Enter)"
                  style={{ flex: 1 }}
                />
                <button type="button" onClick={addSkill} className="btn-glow" style={{ padding: '0 20px', borderRadius: '12px', fontSize: '13px' }}>
                  Add
                </button>
              </div>
              {skills.length > 0 && (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {skills.map(skill => (
                    <span
                      key={skill}
                      className="badge badge-ai"
                      style={{ padding: '6px 12px', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}
                      onClick={() => removeSkill(skill)}
                    >
                      {skill} <X size={12} style={{ opacity: 0.6 }} />
                    </span>
                  ))}
                </div>
              )}
            </div>

            {/* Urgent Toggle */}
            <div style={{ marginBottom: '24px' }}>
              <label
                style={{ display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer', padding: '14px 18px', borderRadius: '12px', border: `1px solid ${isUrgent ? 'rgba(245, 158, 11, 0.3)' : 'var(--border)'}`, background: isUrgent ? 'rgba(245, 158, 11, 0.05)' : 'transparent', transition: 'all 0.2s ease' }}
                onClick={() => setIsUrgent(!isUrgent)}
              >
                <div style={{ width: '40px', height: '22px', borderRadius: '11px', background: isUrgent ? '#F59E0B' : 'rgba(255,255,255,0.1)', position: 'relative', transition: 'background 0.2s ease', flexShrink: 0 }}>
                  <div style={{ width: '18px', height: '18px', borderRadius: '50%', background: 'white', position: 'absolute', top: '2px', left: isUrgent ? '20px' : '2px', transition: 'left 0.2s ease', boxShadow: '0 1px 3px rgba(0,0,0,0.3)' }} />
                </div>
                <div>
                  <span style={{ fontWeight: 600, fontSize: '14px', color: isUrgent ? '#F59E0B' : 'var(--text-primary)' }}>🔥 Mark as Urgent</span>
                  <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>Urgent jobs get highlighted and shown first in the feed</p>
                </div>
              </label>
            </div>

            {/* Submit */}
            <button type="submit" disabled={loading || postSuccess} className="btn btn-primary" style={{ width: '100%', padding: '16px', fontSize: '16px', opacity: (loading || postSuccess) ? 0.7 : 1 }}>
              {loading ? 'Publishing...' : postSuccess ? '✓ Published!' : 'Publish with AI Matching'}
            </button>

            <p style={{ textAlign: 'center', fontSize: '12px', color: 'var(--text-muted)', marginTop: '12px' }}>
              Our AI will auto-match your gig with students who have the right skills
            </p>
          </form>

          {/* Applicants Management Panel */}
          {employerApps.length > 0 && (
            <div>
              <h3 style={{ fontSize: '20px', fontWeight: 600, marginBottom: '24px' }}>Manage Applicants</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {employerApps.map(app => (
                  <div key={app.id} className="spotlight-card" style={{ padding: '20px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                      <div>
                        <span style={{ fontSize: '12px', color: 'var(--primary)', fontWeight: 600, textTransform: 'uppercase' }}>{app.job_title}</span>
                        <h4 style={{ fontSize: '16px', fontWeight: 500 }}>User #{app.user_id} Applied</h4>
                      </div>
                      <span className="badge badge-neutral" style={{ padding: '4px 8px', fontSize: '10px' }}>{app.status.toUpperCase()}</span>
                    </div>

                    {app.status === 'pending' && (
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button onClick={async () => {
                          const res = await fetch(`${API_URL}/api/applications/${app.id}/status`, {
                            method: 'PUT', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                            body: JSON.stringify({ status: 'accepted' })
                          });
                          if (res.ok) { toast('Applicant accepted!', 'success'); refreshEmployerApps(); }
                        }} className="badge badge-ai" style={{ cursor: 'pointer' }}>Accept</button>

                        <button onClick={async () => {
                          const res = await fetch(`${API_URL}/api/applications/${app.id}/status`, {
                            method: 'PUT', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                            body: JSON.stringify({ status: 'rejected' })
                          });
                          if (res.ok) { toast('Applicant rejected', 'info'); refreshEmployerApps(); }
                        }} className="badge badge-neutral" style={{ cursor: 'pointer' }}>Reject</button>
                      </div>
                    )}

                    {app.status === 'checked_in' && (
                      <button onClick={async () => {
                        const res = await fetch(`${API_URL}/api/applications/${app.id}/start-work`, {
                          method: 'POST', headers: { 'Authorization': `Bearer ${token}` }
                        });
                        if (res.ok) { toast('Work started!', 'success'); refreshEmployerApps(); }
                      }} className="btn btn-primary" style={{ padding: '6px 12px', fontSize: '12px' }}>Confirm Arrival (Start Work)</button>
                    )}

                    {app.status === 'work_done' && (
                      <button onClick={async () => {
                        const res = await fetch(`${API_URL}/api/applications/${app.id}/confirm`, {
                          method: 'POST', headers: { 'Authorization': `Bearer ${token}` }
                        });
                        if (res.ok) { toast('Confirmed & paid!', 'success'); refreshEmployerApps(); }
                      }} className="badge badge-ai" style={{ cursor: 'pointer' }}>Confirm Quality & Pay</button>
                    )}

                    {app.status === 'paid' && (
                      <button onClick={async () => {
                        const score = window.prompt('Rate student (1-5 stars):');
                        if (score && parseInt(score) >= 1 && parseInt(score) <= 5) {
                          const res = await fetch(`${API_URL}/api/applications/${app.id}/rate`, {
                            method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                            body: JSON.stringify({ score: parseInt(score), review: '' })
                          });
                          if (res.ok) { toast('Rated successfully!', 'success'); refreshEmployerApps(); } else { toast('Already rated or error occurred.', 'warning'); }
                        }
                      }} className="btn btn-primary" style={{ padding: '6px 12px', fontSize: '12px' }}>Rate Student ⭐</button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  )
}

// --- CMD+K MODAL COMPONENT ---
function CmdKModal() {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setOpen((o) => !o);
      }
      if (e.key === 'Escape') setOpen(false);
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
          style={{ position: 'fixed', inset: 0, zIndex: 999, display: 'flex', alignItems: 'flex-start', justifyContent: 'center', paddingTop: '15vh' }}
        >
          {/* Backdrop */}
          <div
            onClick={() => setOpen(false)}
            style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)' }}
          />

          {/* Modal */}
          <motion.div
            initial={{ scale: 0.95, y: -20 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: -10 }}
            className="spotlight-card"
            style={{ position: 'relative', width: '90%', maxWidth: '600px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border)', borderRadius: '16px', overflow: 'hidden', boxShadow: '0 25px 50px -12px rgba(0,0,0,0.8)' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', padding: '16px 20px', borderBottom: '1px solid var(--border)' }}>
              <Search size={20} color="var(--text-muted)" style={{ marginRight: '16px' }} />
              <input
                autoFocus
                type="text"
                placeholder="What do you want to build or find?"
                value={query} onChange={e => setQuery(e.target.value)}
                style={{ flex: 1, background: 'transparent', border: 'none', color: 'white', fontSize: '18px', outline: 'none' }}
              />
              <span className="mono" style={{ fontSize: '10px', color: 'var(--text-muted)', background: 'rgba(255,255,255,0.05)', padding: '4px 6px', borderRadius: '4px' }}>ESC</span>
            </div>

            <div style={{ padding: '8px', maxHeight: '400px', overflowY: 'auto' }}>
              <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', padding: '12px 12px 4px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Quick Actions</p>

              <Link to="/jobs" onClick={() => setOpen(false)} style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px', borderRadius: '8px', cursor: 'pointer', color: 'white', textDecoration: 'none' }} className="btn-glow">
                <Briefcase size={16} /> Explore Live Jobs
              </Link>
              <Link to="/employers" onClick={() => setOpen(false)} style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px', borderRadius: '8px', cursor: 'pointer', color: 'white', textDecoration: 'none', margin: '8px 0' }} className="btn-glow">
                <Target size={16} /> Post a New Project
              </Link>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

// --- PROFILE PAGE COMPONENT ---
function ProfilePage() {
  const { user, token, setLoginModalOpen } = useAuth();
  const { toast } = useToast();
  const [profile, setProfile] = useState<any>(null);
  const [applications, setApplications] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  // Profile Edit State
  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState('');
  const [editSkills, setEditSkills] = useState(''); // Comma separated string for simplicity

  useEffect(() => {
    if (!token) {
      setLoginModalOpen(true);
      return;
    }

    const fetchAll = async () => {
      setLoading(true);
      try {
        const [profRes, appRes] = await Promise.all([
          fetch(`${API_URL}/api/profile`, { headers: { 'Authorization': `Bearer ${token}` } }),
          fetch(`${API_URL}/api/my-applications`, { headers: { 'Authorization': `Bearer ${token}` } })
        ]);

        if (profRes.ok) {
          const profData = await profRes.json();
          setProfile(profData);
          setEditName(profData.name);
          const parsedSkills = profData.skills_json ? JSON.parse(profData.skills_json) : [];
          setEditSkills(parsedSkills.join(', '));
        }

        if (appRes.ok) {
          setApplications(await appRes.json());
        }
      } catch (err) {
        console.error("Error fetching profile", err);
      } finally {
        setLoading(false);
      }
    };

    fetchAll();
  }, [token, setLoginModalOpen]);

  const refreshApplications = useCallback(async () => {
    if (!token) return;
    try {
      const appRes = await fetch(`${API_URL}/api/my-applications`, { headers: { 'Authorization': `Bearer ${token}` } });
      if (appRes.ok) setApplications(await appRes.json());
    } catch (err) { console.error(err); }
  }, [token]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!token) return;

    try {
      const skillsArray = editSkills.split(',').map(s => s.trim()).filter(Boolean);
      const res = await fetch(`${API_URL}/api/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          name: editName,
          skills_json: JSON.stringify(skillsArray)
        })
      });

      if (res.ok) {
        const updatedProf = await res.json();
        setProfile(updatedProf);
        setIsEditing(false);
        toast('Profile updated successfully!', 'success');
      } else {
        toast('Failed to update profile.', 'error');
      }
    } catch (err) {
      toast('Error updating profile. Please try again.', 'error');
    }
  };

  if (!user || !token) {
    return <div style={{ padding: '100px 0', textAlign: 'center' }}>Please log in to view your profile.</div>;
  }

  if (loading) {
    return <div style={{ padding: '100px 0', textAlign: 'center' }}><div className="animate-float" style={{ fontSize: '24px', color: 'var(--primary)' }}>Loading intelligently...</div></div>;
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} style={{ padding: '40px 0', minHeight: '80vh' }}>
      <div className="page-container" style={{ maxWidth: '900px' }}>
        <h1 style={{ fontSize: '32px', fontWeight: 700, letterSpacing: '-0.02em', marginBottom: '32px' }}>Your Profile</h1>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '32px' }}>
          {/* Sidebar: Profile Info */}
          <div className="spotlight-card" style={{ padding: '24px', height: 'fit-content' }}>
            {isEditing ? (
              <form onSubmit={handleUpdateProfile}>
                <h3 style={{ fontSize: '18px', fontWeight: 600, marginBottom: '16px' }}>Edit Profile</h3>
                <label style={{ display: 'block', marginBottom: '12px', fontSize: '14px', color: 'var(--text-secondary)' }}>
                  Name
                  <input className="input-field" value={editName} onChange={(e) => setEditName(e.target.value)} style={{ marginTop: '8px' }} required />
                </label>
                <label style={{ display: 'block', marginBottom: '16px', fontSize: '14px', color: 'var(--text-secondary)' }}>
                  Skills (comma separated)
                  <input className="input-field" value={editSkills} onChange={(e) => setEditSkills(e.target.value)} style={{ marginTop: '8px' }} placeholder="e.g. React, Python, Data Entry" />
                </label>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button type="button" onClick={() => setIsEditing(false)} className="badge badge-neutral" style={{ padding: '8px 16px', background: 'transparent', cursor: 'pointer', flex: 1, justifyContent: 'center' }}>Cancel</button>
                  <button type="submit" className="badge badge-ai" style={{ padding: '8px 16px', cursor: 'pointer', flex: 1, justifyContent: 'center' }}>Save</button>
                </div>
              </form>
            ) : (
              <>
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '24px' }}>
                  <div style={{ width: '64px', height: '64px', borderRadius: '16px', background: 'var(--bg-surface-elevated)', border: '1px solid var(--border)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px' }}>
                    <User color="var(--primary)" />
                  </div>
                  <div>
                    <h2 style={{ fontSize: '20px', fontWeight: 700 }}>{profile?.name}</h2>
                    <p style={{ color: 'var(--text-muted)', fontSize: '14px' }}>{profile?.phone}</p>
                    {profile?.trust_badge && <span className="badge badge-ai" style={{ marginTop: '4px' }}>{profile.trust_badge}</span>}
                  </div>
                </div>

                <div style={{ marginBottom: '24px' }}>
                  <h4 style={{ fontSize: '14px', color: 'var(--text-muted)', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Your AI Skills</h4>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    {profile?.skills_json && JSON.parse(profile.skills_json).map((skill: string) => (
                      <span key={skill} className="badge badge-neutral" style={{ padding: '4px 10px', fontSize: '11px' }}>{skill}</span>
                    ))}
                    {(!profile || !profile.skills_json || JSON.parse(profile.skills_json).length === 0) && (
                      <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>No skills added. Add them to get AI matches!</span>
                    )}
                  </div>
                </div>

                <div style={{ background: 'rgba(0,0,0,0.3)', padding: '16px', borderRadius: '12px', border: '1px solid var(--border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span style={{ color: 'var(--text-muted)', fontSize: '14px' }}>Total Earned</span>
                    <span className="text-gradient-ai" style={{ fontWeight: 700 }}>₹{profile?.total_earned ? profile.total_earned.toFixed(2) : '0.00'}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--text-muted)', fontSize: '14px' }}>Gigs Completed</span>
                    <span style={{ fontWeight: 600 }}>{profile?.gigs_completed || 0}</span>
                  </div>
                </div>

                <button onClick={() => setIsEditing(true)} className="btn-glow" style={{ width: '100%', padding: '12px', borderRadius: '8px', marginTop: '24px', background: 'transparent', color: 'white', border: '1px solid var(--border)', cursor: 'pointer' }}>
                  Edit Profile
                </button>
              </>
            )}
          </div>

          {/* Main Area: Applications */}
          <div>
            <h3 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '24px' }}>Your Job Applications</h3>

            {applications.length === 0 ? (
              <div className="spotlight-card" style={{ padding: '48px 32px', textAlign: 'center' }}>
                <div style={{ width: '80px', height: '80px', borderRadius: '20px', background: 'rgba(139, 92, 246, 0.08)', border: '1px solid rgba(139, 92, 246, 0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px' }}>
                  <Rocket size={36} color="var(--primary)" />
                </div>
                <h3 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '8px', letterSpacing: '-0.01em' }}>Ready to launch your career?</h3>
                <p style={{ color: 'var(--text-secondary)', marginBottom: '8px', maxWidth: '340px', margin: '0 auto 8px', lineHeight: 1.6 }}>You haven't applied to any gigs yet. Our AI engine is ready to match you with the perfect opportunity.</p>
                <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginBottom: '24px' }}>Tip: Add skills to your profile for better AI matches! 🎯</p>
                <Link to="/jobs" className="btn btn-primary" style={{ padding: '12px 24px', fontSize: '15px' }}>
                  <Search size={16} /> Find Your First Gig
                </Link>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <AnimatePresence>
                  {applications.map((app) => (
                    <motion.div
                      key={app.id}
                      className="spotlight-card"
                      style={{ padding: '24px' }}
                      initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div>
                          <h4 style={{ fontSize: '18px', fontWeight: 600, marginBottom: '4px' }}>{app.job_title}</h4>
                          <p style={{ fontSize: '14px', color: 'var(--text-muted)' }}>📍 {app.job_location} • Budget: ₹{app.job_pay_amount}</p>
                        </div>
                        <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px' }}>
                          <span className="badge badge-neutral" style={{ padding: '6px 12px', background: app.status === 'accepted' ? 'rgba(16, 185, 129, 0.1)' : app.status === 'rejected' ? 'rgba(239, 68, 68, 0.1)' : app.status === 'work_done' ? 'rgba(56, 189, 248, 0.1)' : 'rgba(255,255,255,0.05)', color: app.status === 'accepted' ? '#10B981' : app.status === 'rejected' ? '#EF4444' : app.status === 'work_done' ? '#38BDF8' : 'var(--text-secondary)', borderColor: app.status === 'accepted' ? 'rgba(16, 185, 129, 0.2)' : app.status === 'rejected' ? 'rgba(239, 68, 68, 0.2)' : app.status === 'work_done' ? 'rgba(56, 189, 248, 0.2)' : 'rgba(255,255,255,0.1)' }}>
                            {app.status.toUpperCase().replace('_', ' ')}
                          </span>

                          {/* Student UI Lifecycle Buttons */}
                          {app.status === 'accepted' && (
                            <button
                              onClick={async () => {
                                const res = await fetch(`${API_URL}/api/applications/${app.id}/check-in`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` } });
                                if (res.ok) { toast('Checked in!', 'success'); refreshApplications(); }
                              }}
                              className="btn btn-primary" style={{ padding: '4px 12px', fontSize: '12px' }}>
                              I've Arrived (Check-In)
                            </button>
                          )}
                          {app.status === 'in_progress' && (
                            <button
                              onClick={async () => {
                                const res = await fetch(`${API_URL}/api/applications/${app.id}/complete`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` } });
                                if (res.ok) { toast('Work marked as done!', 'success'); refreshApplications(); }
                              }}
                              className="badge badge-ai" style={{ cursor: 'pointer' }}>
                              Mark Work Done
                            </button>
                          )}
                          {app.status === 'confirmed' && (
                            <button
                              onClick={async () => {
                                const res = await fetch(`${API_URL}/api/applications/${app.id}/confirm-payment`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` } });
                                if (res.ok) { toast('Payment confirmed!', 'success'); refreshApplications(); }
                              }}
                              className="badge badge-ai" style={{ cursor: 'pointer' }}>
                              Confirm Payment Received
                            </button>
                          )}
                          {app.status === 'paid' && (
                            <button
                              onClick={async () => {
                                const score = window.prompt('Rate employer (1-5 stars):');
                                if (score && parseInt(score) >= 1 && parseInt(score) <= 5) {
                                  const res = await fetch(`${API_URL}/api/applications/${app.id}/rate`, {
                                    method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
                                    body: JSON.stringify({ score: parseInt(score), review: '' })
                                  });
                                  if (res.ok) { toast('Rated successfully!', 'success'); refreshApplications(); } else { toast('Already rated or error occurred.', 'warning'); }
                                }
                              }}
                              className="btn btn-primary" style={{ padding: '4px 12px', fontSize: '12px' }}>
                              Rate Employer ⭐
                            </button>
                          )}

                          <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                            Applied: {new Date(app.applied_at).toLocaleDateString()}
                          </p>
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>
              </div>
            )}
          </div>
        </div>
      </div>
    </motion.div>
  );
}

// --- LOGIN MODAL COMPONENT (Premium) ---
function LoginModal() {
  const { isLoginModalOpen, setLoginModalOpen, setAuthState } = useAuth();
  const { toast } = useToast();

  const [mode, setMode] = useState<'login' | 'register' | 'forgot'>('login');
  const [phone, setPhone] = useState('');
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');
  const [secQuestion, setSecQuestion] = useState('');
  const [secAnswer, setSecAnswer] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [role, setRole] = useState<'student' | 'employer'>('student');
  const [loading, setLoading] = useState(false);
  const [forgotStep, setForgotStep] = useState(1);
  const [showPassword, setShowPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const markTouched = (field: string) => setTouched(prev => ({ ...prev, [field]: true }));

  const switchMode = (newMode: 'login' | 'register' | 'forgot') => {
    setMode(newMode);
    setTouched({});
    setShowPassword(false);
    setShowNewPassword(false);
    if (newMode === 'forgot') setForgotStep(1);
  };

  // --- Validation ---
  const validateName = (n: string): string => {
    const trimmed = n.trim();
    if (!trimmed) return 'Name is required';
    if (trimmed.length < 3) return 'Name must be at least 3 characters';
    const dummies = ["abc", "xyz", "test", "demo", "asdf", "qwerty", "admin", "user", "name", "hello"];
    if (dummies.includes(trimmed.toLowerCase())) return 'Please enter your real name';
    if (trimmed.length >= 4 && new Set(trimmed.toLowerCase().replace(/ /g, '')).size <= 1) return 'Name cannot be all the same character';
    if (!/^[a-zA-Z ]+$/.test(trimmed)) return 'Only letters and spaces allowed';
    return '';
  };

  const validatePhone = (p: string): string => {
    if (!p) return 'Phone number is required';
    if (p.length < 10) return `${10 - p.length} more digit${10 - p.length > 1 ? 's' : ''} needed`;
    if (!/^[6-9]/.test(p)) return 'Indian mobile numbers start with 6, 7, 8, or 9';
    if (new Set(p).size <= 2) return 'Too many repeated digits';
    if ('0123456789'.includes(p) || '9876543210'.includes(p)) return 'Sequential patterns are not allowed';
    return '';
  };

  const validatePassword = (pw: string, isReg: boolean): string => {
    if (!pw) return 'Password is required';
    if (isReg && pw.length < 6) return `${6 - pw.length} more character${6 - pw.length > 1 ? 's' : ''} needed`;
    if (!isReg && pw.length < 4) return 'Password must be at least 4 characters';
    return '';
  };

  const validateSecQ = (q: string): string => {
    if (!q.trim()) return 'Security question is required';
    if (q.trim().length < 5) return 'Must be at least 5 characters';
    return '';
  };

  const validateSecA = (a: string): string => {
    if (!a.trim()) return 'Answer is required';
    if (a.trim().length < 3) return 'Must be at least 3 characters';
    return '';
  };

  const getStrength = (pw: string) => {
    if (pw.length < 6) return { level: 'weak' as const, label: 'Too short' };
    const has = { letters: /[a-zA-Z]/.test(pw), nums: /[0-9]/.test(pw), special: /[^a-zA-Z0-9]/.test(pw) };
    if (pw.length >= 8 && has.letters && has.nums && has.special) return { level: 'strong' as const, label: 'Strong' };
    if (has.letters && has.nums) return { level: 'medium' as const, label: 'Medium' };
    return { level: 'weak' as const, label: 'Weak' };
  };

  const nameErr = validateName(name);
  const phoneErr = validatePhone(phone);
  const pwErr = validatePassword(password, mode === 'register');
  const sqErr = validateSecQ(secQuestion);
  const saErr = validateSecA(secAnswer);
  const pwStrength = getStrength(password);

  const fieldCls = (err: string, field: string, val: string) => {
    if (!touched[field] || !val) return 'input-field';
    return `input-field ${err ? 'field-error' : 'field-success'}`;
  };

  const feedback = (err: string, field: string, val: string) => {
    if (!touched[field] || !val) return <div className="field-success-text" style={{ visibility: 'hidden' }}><CheckCircle2 size={12} />_</div>;
    if (err) return <div className="field-error-text"><AlertCircle size={12} />{err}</div>;
    return <div className="field-success-text"><CheckCircle2 size={12} />Looks good!</div>;
  };

  if (!isLoginModalOpen) return null;

  // --- Handlers ---
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setTouched({ phone: true, password: true });
    if (phoneErr || pwErr) { toast('Please fix the errors above.', 'error'); return; }
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/login`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone, password })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(typeof data.detail === 'string' ? data.detail : 'Login failed');
      setAuthState(data.access_token, data.user);
      setLoginModalOpen(false);
      toast('Welcome back! 🎉', 'success');
    } catch (err: any) { toast(err.message, 'error'); }
    finally { setLoading(false); }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setTouched({ name: true, phone: true, password: true, secQuestion: true, secAnswer: true });
    if (nameErr || phoneErr || pwErr || sqErr || saErr) { toast('Please fix the highlighted errors.', 'error'); return; }
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/register`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone, name, password, security_question: secQuestion, security_answer: secAnswer, role })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(typeof data.detail === 'string' ? data.detail : 'Registration failed');
      setAuthState(data.access_token, data.user);
      setLoginModalOpen(false);
      toast('Account created! 🎉', 'success');
    } catch (err: any) { toast(err.message, 'error'); }
    finally { setLoading(false); }
  };

  const handleForgotStep1 = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/auth/reset-get-question`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.detail || 'Account not found');
      setSecQuestion(data.question);
      setForgotStep(2);
    } catch (err: any) { toast(err.message, 'error'); }
    finally { setLoading(false); }
  };

  const handleForgotStep2 = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/api/auth/reset-password`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ phone, security_answer: secAnswer, new_password: newPassword })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(typeof data.detail === 'string' ? data.detail : 'Reset failed');
      toast('Password reset! Please log in.', 'success');
      switchMode('login');
      setPassword('');
    } catch (err: any) { toast(err.message, 'error'); }
    finally { setLoading(false); }
  };



  return (
    <div className="auth-modal-backdrop" onClick={() => setLoginModalOpen(false)}>
      <motion.div
        key={mode}
        initial={{ scale: 0.95, y: -20, opacity: 0 }}
        animate={{ scale: 1, y: 0, opacity: 1 }}
        transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
        className="auth-modal"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close */}
        <button onClick={() => setLoginModalOpen(false)} style={{ position: 'absolute', top: '16px', right: '16px', background: 'rgba(255,255,255,0.05)', border: '1px solid var(--border)', borderRadius: '10px', padding: '8px', cursor: 'pointer', color: 'var(--text-muted)', display: 'flex', transition: 'all 0.2s' }}>
          <X size={16} />
        </button>

        {/* Header Icon */}
        <div className={`auth-header-icon ${mode === 'login' ? 'login-icon' : mode === 'register' ? 'register-icon' : 'forgot-icon'}`}>
          {mode === 'login' && <Zap color="var(--primary)" size={28} />}
          {mode === 'register' && <UserPlus color="#10B981" size={28} />}
          {mode === 'forgot' && <Lock color="var(--accent)" size={28} />}
        </div>

        <h2 style={{ fontSize: '26px', fontWeight: 800, letterSpacing: '-0.03em', marginBottom: '6px' }}>
          {mode === 'login' ? 'Welcome back' : mode === 'register' ? 'Create Account' : 'Reset Password'}
        </h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '28px', fontSize: '14px', lineHeight: 1.5 }}>
          {mode === 'login' ? 'Sign in to access your StudentGig dashboard.' : mode === 'register' ? 'Join thousands of students earning on their schedule.' : 'Recover your account using your security question.'}
        </p>

        {/* ========= LOGIN ========= */}
        {mode === 'login' && (
          <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            <div className="auth-field-group">
              <div className="auth-input-wrapper">
                <Smartphone size={16} className="auth-input-icon" />
                <input className={fieldCls(phoneErr, 'phone', phone)} type="tel" required placeholder="Phone Number" value={phone} onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))} onBlur={() => markTouched('phone')} autoFocus style={{ paddingLeft: '44px' }} />
              </div>
              {feedback(phoneErr, 'phone', phone)}
            </div>

            <div className="auth-field-group">
              <div className="auth-input-wrapper">
                <Lock size={16} className="auth-input-icon" />
                <input className={fieldCls(pwErr, 'password', password)} type={showPassword ? 'text' : 'password'} required placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} onBlur={() => markTouched('password')} style={{ paddingLeft: '44px', paddingRight: '44px' }} />
                <button type="button" className="auth-password-toggle" onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {feedback(pwErr, 'password', password)}
            </div>

            <div style={{ textAlign: 'right', marginBottom: '8px' }}>
              <button type="button" onClick={() => switchMode('forgot')} style={{ background: 'transparent', border: 'none', color: 'var(--primary)', cursor: 'pointer', fontSize: '12px', fontWeight: 600 }}>Forgot Password?</button>
            </div>

            <button type="submit" className="auth-submit-btn login-btn" disabled={loading}>
              {loading ? <><div className="auth-spinner" /> Signing in...</> : <><Lock size={16} /> Sign In</>}
            </button>



            <div className="auth-mode-toggle">
              Don't have an account? <button type="button" onClick={() => switchMode('register')}>Create one</button>
            </div>
          </form>
        )}

        {/* ========= REGISTER ========= */}
        {mode === 'register' && (
          <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            {/* Role Cards */}
            <div className="auth-role-selector">
              <div className={`auth-role-card ${role === 'student' ? 'active' : ''}`} onClick={() => setRole('student')}>
                <div className="role-icon"><GraduationCap size={18} color={role === 'student' ? 'var(--primary)' : 'var(--text-muted)'} /></div>
                <span className="role-title">Student</span>
                <span className="role-desc">Find Gigs</span>
              </div>
              <div className={`auth-role-card ${role === 'employer' ? 'active' : ''}`} onClick={() => setRole('employer')}>
                <div className="role-icon"><Building2 size={18} color={role === 'employer' ? 'var(--primary)' : 'var(--text-muted)'} /></div>
                <span className="role-title">Employer</span>
                <span className="role-desc">Hire Talent</span>
              </div>
            </div>

            <div className="auth-section-label"><User size={12} /> Identity</div>

            <div className="auth-field-group">
              <div className="auth-input-wrapper">
                <User size={16} className="auth-input-icon" />
                <input className={fieldCls(nameErr, 'name', name)} required placeholder="Full Name" value={name} onChange={(e) => setName(e.target.value.replace(/[^a-zA-Z ]/g, '').slice(0, 50))} onBlur={() => markTouched('name')} autoFocus style={{ paddingLeft: '44px' }} />
              </div>
              {feedback(nameErr, 'name', name)}
            </div>

            <div className="auth-field-group">
              <div className="auth-input-wrapper">
                <Smartphone size={16} className="auth-input-icon" />
                <input className={fieldCls(phoneErr, 'phone', phone)} type="tel" required placeholder="10-Digit Phone Number" value={phone} onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))} onBlur={() => markTouched('phone')} style={{ paddingLeft: '44px' }} />
              </div>
              {feedback(phoneErr, 'phone', phone)}
            </div>

            <div className="auth-section-label"><Shield size={12} /> Security</div>

            <div className="auth-field-group">
              <div className="auth-input-wrapper">
                <Lock size={16} className="auth-input-icon" />
                <input className={fieldCls(pwErr, 'password', password)} type={showPassword ? 'text' : 'password'} required placeholder="Create Password (min 6 chars)" value={password} onChange={(e) => setPassword(e.target.value)} onBlur={() => markTouched('password')} minLength={6} style={{ paddingLeft: '44px', paddingRight: '44px' }} />
                <button type="button" className="auth-password-toggle" onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {password.length > 0 && (
                <>
                  <div className="password-strength-bar"><div className={`password-strength-fill str-${pwStrength.level}`} /></div>
                  <div className={`password-strength-label str-${pwStrength.level}`}>{pwStrength.label}</div>
                </>
              )}
              {feedback(pwErr, 'password', password)}
            </div>

            <div className="auth-field-group">
              <div className="auth-input-wrapper">
                <Shield size={16} className="auth-input-icon" />
                <input className={fieldCls(sqErr, 'secQuestion', secQuestion)} required placeholder="Security Question (e.g. Your birthplace?)" value={secQuestion} onChange={(e) => setSecQuestion(e.target.value)} onBlur={() => markTouched('secQuestion')} style={{ paddingLeft: '44px' }} />
              </div>
              {feedback(sqErr, 'secQuestion', secQuestion)}
            </div>

            <div className="auth-field-group">
              <div className="auth-input-wrapper">
                <Key size={16} className="auth-input-icon" />
                <input className={fieldCls(saErr, 'secAnswer', secAnswer)} required placeholder="Secret Answer" value={secAnswer} onChange={(e) => setSecAnswer(e.target.value.slice(0, 50))} onBlur={() => markTouched('secAnswer')} style={{ paddingLeft: '44px' }} />
              </div>
              {feedback(saErr, 'secAnswer', secAnswer)}
            </div>

            <button type="submit" className="auth-submit-btn register-btn" disabled={loading} style={{ marginTop: '8px' }}>
              {loading ? <><div className="auth-spinner" /> Creating account...</> : <><Rocket size={16} /> Create Account</>}
            </button>



            <div className="auth-mode-toggle">
              Already have an account? <button type="button" onClick={() => switchMode('login')}>Sign in</button>
            </div>
          </form>
        )}

        {/* ========= FORGOT PASSWORD ========= */}
        {mode === 'forgot' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {forgotStep === 1 ? (
              <form onSubmit={handleForgotStep1} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div className="auth-input-wrapper">
                  <Smartphone size={16} className="auth-input-icon" />
                  <input className="input-field" type="tel" required placeholder="Phone Number" value={phone} onChange={(e) => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))} autoFocus style={{ paddingLeft: '44px' }} />
                </div>
                <button type="submit" className="auth-submit-btn login-btn" disabled={loading}>
                  {loading ? <><div className="auth-spinner" /> Finding account...</> : <>Next <ChevronRight size={16} /></>}
                </button>
              </form>
            ) : (
              <form onSubmit={handleForgotStep2} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div style={{ background: 'rgba(139, 92, 246, 0.06)', padding: '14px 16px', borderRadius: '12px', fontSize: '14px', color: 'var(--text-primary)', border: '1px solid rgba(139, 92, 246, 0.15)', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Shield size={16} color="var(--primary)" />
                  <div>
                    <span style={{ fontSize: '11px', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase' as const, letterSpacing: '0.05em' }}>Security Question</span>
                    <p style={{ fontWeight: 600, marginTop: '2px' }}>{secQuestion}</p>
                  </div>
                </div>
                <div className="auth-input-wrapper">
                  <Key size={16} className="auth-input-icon" />
                  <input className="input-field" required placeholder="Your Answer" value={secAnswer} onChange={(e) => setSecAnswer(e.target.value)} autoFocus style={{ paddingLeft: '44px' }} />
                </div>
                <div className="auth-input-wrapper">
                  <Lock size={16} className="auth-input-icon" />
                  <input className="input-field" type={showNewPassword ? 'text' : 'password'} required placeholder="New Password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} style={{ paddingLeft: '44px', paddingRight: '44px' }} />
                  <button type="button" className="auth-password-toggle" onClick={() => setShowNewPassword(!showNewPassword)}>
                    {showNewPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
                <button type="submit" className="auth-submit-btn login-btn" disabled={loading}>
                  {loading ? <><div className="auth-spinner" /> Resetting...</> : <><Lock size={16} /> Reset Password</>}
                </button>
              </form>
            )}
            <div style={{ textAlign: 'center' }}>
              <button type="button" onClick={() => switchMode('login')} style={{ background: 'transparent', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '6px', margin: '0 auto' }}>
                <ChevronRight size={14} style={{ transform: 'rotate(180deg)' }} /> Back to Login
              </button>
            </div>
          </div>
        )}
      </motion.div>
    </div>
  );
}

// --- FOOTER COMPONENT ---
function Footer() {
  return (
    <footer className="site-footer">
      <div className="page-container">
        <div className="footer-grid">
          <div className="footer-brand">
            <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none', color: 'white', marginBottom: '4px' }}>
              <div style={{ width: '28px', height: '28px', borderRadius: '8px', background: 'linear-gradient(135deg, #FFFFFF, #A1A1AA)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Zap color="black" size={14} fill="black" />
              </div>
              <span style={{ fontSize: '16px', fontWeight: 700, letterSpacing: '-0.02em' }}>StudentGig</span>
            </Link>
            <p>AI-powered platform connecting students with real-world gig opportunities. Built with ♥ for the next generation of freelancers.</p>
          </div>

          <div className="footer-col">
            <h4>Platform</h4>
            <Link to="/jobs">Find Gigs</Link>
            <Link to="/employers">Post a Job</Link>
            <Link to="/profile">My Profile</Link>
          </div>

          <div className="footer-col">
            <h4>Resources</h4>
            <a href="#">How It Works</a>
            <a href="#">AI Matching</a>
            <a href="#">Trust & Safety</a>
          </div>

          <div className="footer-col">
            <h4>Connect</h4>
            <a href="#" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Mail size={14} /> Support</a>
            <a href="#" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Twitter size={14} /> Twitter</a>
            <a href="#" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Linkedin size={14} /> LinkedIn</a>
          </div>
        </div>

        <div className="footer-bottom">
          <span>© {new Date().getFullYear()} StudentGig. All rights reserved.</span>
          <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            Made with <Heart size={12} color="#EF4444" fill="#EF4444" /> in India
          </span>
        </div>
      </div>
    </footer>
  );
}
