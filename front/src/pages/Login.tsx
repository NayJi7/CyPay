import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Swal from 'sweetalert2';

const swalDark = Swal.mixin({
  background: '#0f172a',
  color: '#e2e8f0',
  confirmButtonColor: '#06b6d4',
  cancelButtonColor: '#ef4444',
  buttonsStyling: true,
  customClass: {
    popup: 'border border-slate-700 rounded-2xl shadow-[0_0_50px_rgba(0,0,0,0.5)]',
    title: 'font-bold text-xl',
    htmlContainer: 'text-slate-400'
  }
});

const Login: React.FC = () => {
  const [isRegistering, setIsRegistering] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [pseudo, setPseudo] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async () => {
    if (!email || !password || (isRegistering && !pseudo)) {
      setError('Veuillez remplir tous les champs');
      return;
    }

    if (!email.includes('@')) {
      setError('Email invalide');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const endpoint = isRegistering ? '/users/register' : '/users/login';
      const body = isRegistering 
        ? { email, password, pseudo }
        : { email, password };

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Une erreur est survenue');
      }

      if (isRegistering) {
        setIsRegistering(false);
        setError('');
        swalDark.fire({
          icon: 'success',
          title: 'Inscription Réussie',
          text: 'Compte créé avec succès ! Connectez-vous.',
          timer: 3000
        });
      } else {
        localStorage.setItem('token', data.token);
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('userEmail', data.email);
        localStorage.setItem('userPseudo', data.pseudo);
        localStorage.setItem('isLoggedIn', 'true');
        
        navigate('/dashboard');
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSubmit();
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4 relative overflow-hidden font-sans">
      {/* Background Ambience */}
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden z-0 pointer-events-none">
        <div className="absolute top-[-10%] right-[-5%] w-96 h-96 bg-purple-600/20 rounded-full blur-[100px]"></div>
        <div className="absolute bottom-[-10%] left-[-5%] w-96 h-96 bg-cyan-600/20 rounded-full blur-[100px]"></div>
        <div className="absolute top-[40%] left-[30%] w-64 h-64 bg-pink-600/10 rounded-full blur-[80px]"></div>
      </div>

      <div className="max-w-md w-full relative z-10">
        <div className="backdrop-blur-xl bg-slate-900/60 border border-white/10 rounded-3xl shadow-[0_0_50px_rgba(0,0,0,0.5)] overflow-hidden">
          
          {/* Header Section */}
          <div className="p-8 text-center border-b border-white/5 bg-gradient-to-b from-white/5 to-transparent">
            <div className="inline-flex items-center justify-center w-24 h-24 rounded-full bg-gradient-to-tr from-cyan-500 to-purple-600 mb-6 shadow-lg shadow-cyan-500/20 overflow-hidden">
              <img src="/icon.jpg" alt="Logo" className="w-full h-full object-cover" />
            </div>
            <h1 className="text-4xl font-black text-transparent bg-clip-text bg-gradient-to-r from-white via-cyan-100 to-slate-400 tracking-tight">
              CY<span className="text-cyan-500">PAY</span>
            </h1>
            <p className="text-slate-400 mt-2 text-sm font-mono tracking-widest uppercase">Votre système de paiement et d'investissement sécurisé</p>
          </div>

          {/* Form Section */}
          <div className="p-8 pt-6">
            <h2 className="text-xl font-bold text-white mb-6 flex items-center">
              <span className="w-1 h-6 bg-cyan-500 rounded-full mr-3 shadow-[0_0_10px_#06b6d4]"></span>
              {isRegistering ? 'INITIALISATION IDENTITÉ' : 'AUTHENTIFICATION'}
            </h2>

            {error && (
              <div className="mb-6 p-4 bg-red-500/10 border-l-4 border-red-500 text-red-200 text-sm font-mono flex items-center">
                <span className="mr-2">⚠</span> {error}
              </div>
            )}

            <div className="space-y-5">
              {isRegistering && (
                <div className="group">
                  <label className="block text-slate-400 text-xs font-bold uppercase tracking-wider mb-2 group-focus-within:text-cyan-400 transition-colors">Identifiant Pseudo</label>
                  <input
                    type="text"
                    value={pseudo}
                    onChange={(e) => setPseudo(e.target.value)}
                    onKeyPress={handleKeyPress}
                    placeholder="ex: Satoshi_N"
                    className="w-full bg-slate-950/50 border border-slate-700 rounded-xl px-4 py-3.5 text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/50 transition-all font-mono"
                  />
                </div>
              )}

              <div className="group">
                <label className="block text-slate-400 text-xs font-bold uppercase tracking-wider mb-2 group-focus-within:text-cyan-400 transition-colors">Adresse Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="name@secure.net"
                  className="w-full bg-slate-950/50 border border-slate-700 rounded-xl px-4 py-3.5 text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/50 transition-all font-mono"
                />
              </div>

              <div className="group">
                <label className="block text-slate-400 text-xs font-bold uppercase tracking-wider mb-2 group-focus-within:text-cyan-400 transition-colors">Clé de sécurité</label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="••••••••••••"
                  className="w-full bg-slate-950/50 border border-slate-700 rounded-xl px-4 py-3.5 text-white placeholder-slate-600 focus:outline-none focus:border-cyan-500 focus:ring-1 focus:ring-cyan-500/50 transition-all font-mono"
                />
              </div>

              <button
                onClick={handleSubmit}
                disabled={loading}
                className="w-full mt-4 bg-gradient-to-r from-cyan-600 to-blue-700 hover:from-cyan-500 hover:to-blue-600 text-white font-bold py-4 rounded-xl shadow-lg shadow-cyan-900/50 hover:shadow-cyan-500/30 transition-all transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center relative overflow-hidden group"
              >
                <div className="absolute inset-0 w-full h-full bg-white/20 -translate-x-full group-hover:translate-x-full transition-transform duration-700 skew-x-12"></div>
                {loading ? (
                  <span className="flex items-center font-mono">
                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-cyan-200" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    TRAITEMENT...
                  </span>
                ) : (
                  <span className="tracking-widest">{isRegistering ? "CONFIRMER L'INSCRIPTION" : "CONNEXION SÉCURISÉE"}</span>
                )}
              </button>
            </div>

            <div className="mt-8 text-center border-t border-white/5 pt-6">
              <button
                onClick={() => {
                  setIsRegistering(!isRegistering);
                  setError('');
                }}
                className="text-slate-400 hover:text-cyan-400 text-sm transition-colors font-mono hover:underline decoration-cyan-500/50 underline-offset-4"
              >
                {isRegistering 
                  ? "< Retour à la connexion" 
                  : "Nouveau membre ? Créer un identifiant >"}
              </button>
            </div>
          </div>
        </div>
        
        <div className="text-center mt-6 text-slate-600 text-xs font-mono">
          SECURED BY BLOCKCHAIN TECHNOLOGY • E2E ENCRYPTED
        </div>
      </div>
    </div>
  );
};

export default Login;