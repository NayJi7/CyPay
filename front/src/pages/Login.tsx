import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

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
        // Après inscription, on passe en mode connexion
        setIsRegistering(false);
        setError('');
        alert('Compte créé avec succès ! Connectez-vous.');
      } else {
        // Connexion réussie
        localStorage.setItem('token', data.token);
        localStorage.setItem('userId', data.id);
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
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center px-4">
      <div className="max-w-md w-full">
        {/* Titre */}
        <div className="text-center mb-8">
          <h1 className="text-5xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-purple-400 to-pink-600 mb-2">CyPay</h1>
          <p className="text-gray-400">La crypto-banque de nouvelle génération</p>
        </div>

        {/* Formulaire */}
        <div className="bg-white/10 backdrop-blur-xl rounded-2xl p-8 border border-white/20 shadow-2xl">
          <h2 className="text-2xl font-bold text-white mb-6 text-center">
            {isRegistering ? 'Créer un compte' : 'Connexion'}
          </h2>

          {error && (
            <div className="bg-red-500/20 border border-red-500/50 rounded-lg p-3 mb-6 animate-pulse">
              <p className="text-red-200 text-sm text-center">{error}</p>
            </div>
          )}

          <div className="space-y-5">
            {isRegistering && (
              <div>
                <label className="block text-gray-300 mb-2 text-sm font-medium">Pseudo</label>
                <input
                  type="text"
                  value={pseudo}
                  onChange={(e) => setPseudo(e.target.value)}
                  onKeyPress={handleKeyPress}
                  placeholder="CryptoKing"
                  className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
                />
              </div>
            )}

            <div>
              <label className="block text-gray-300 mb-2 text-sm font-medium">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="ton@email.com"
                className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
              />
            </div>

            <div>
              <label className="block text-gray-300 mb-2 text-sm font-medium">Mot de passe</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="••••••••"
                className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
              />
            </div>

            <button
              onClick={handleSubmit}
              disabled={loading}
              className="w-full bg-gradient-to-r from-purple-600 to-pink-600 text-white font-bold py-3.5 rounded-lg hover:from-purple-500 hover:to-pink-500 transition-all transform hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-purple-500/25"
            >
              {loading ? (
                <span className="flex items-center justify-center">
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Chargement...
                </span>
              ) : (
                isRegistering ? "S'inscrire" : "Se connecter"
              )}
            </button>

            <div className="text-center mt-6">
              <button
                onClick={() => {
                  setIsRegistering(!isRegistering);
                  setError('');
                }}
                className="text-gray-400 hover:text-white text-sm transition-colors underline decoration-dotted underline-offset-4"
              >
                {isRegistering 
                  ? "Déjà un compte ? Se connecter" 
                  : "Pas encore de compte ? S'inscrire"}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;