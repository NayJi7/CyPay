import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

interface Wallet {
  id: number;
  userId: number;
  currency: string;
  balance: number;
}

interface Transaction {
  id: string;
  type: 'buy' | 'sell' | 'transfer';
  crypto?: string;
  currency?: string;
  amount: number;
  recipient?: string;
  date: string;
}

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [wallets, setWallets] = useState<Wallet[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Mock prices for now since we don't have a price feed API
  const [prices] = useState<Record<string, number>>({
    BTC: 45000,
    ETH: 3000,
    SOL: 100,
    EUR: 1,
    USD: 0.92,
  });

  const [transactionForm, setTransactionForm] = useState({
    crypto: '',
    currency: 'EUR',
    amount: '',
    recipient: '',
    type: 'buy' as 'buy' | 'sell' | 'transfer',
    mode: 'crypto' as 'crypto' | 'fiat',
    transferType: 'crypto' as 'crypto' | 'fiat',
  });

  const [userProfile, setUserProfile] = useState({
    name: '',
    email: '',
    avatar: ''
  });

  // Mock history since no API endpoint exists for it yet
  const [transactionHistory] = useState<Transaction[]>([
    { id: '1', type: 'buy', crypto: 'BTC', amount: 0.2, date: '2024-12-05' },
    { id: '2', type: 'transfer', currency: 'EUR', amount: 500, recipient: 'alice@email.com', date: '2024-12-05' },
  ]);

  useEffect(() => {
    const userId = localStorage.getItem('userId');
    const userEmail = localStorage.getItem('userEmail');
    const userPseudo = localStorage.getItem('userPseudo');
    const isLoggedIn = localStorage.getItem('isLoggedIn');

    if (!isLoggedIn || !userId) {
      navigate('/');
      return;
    }

    setUserProfile({
      name: userPseudo || 'Utilisateur',
      email: userEmail || '',
      avatar: `https://ui-avatars.com/api/?name=${userPseudo || 'User'}&background=8b5cf6&color=fff&size=128`
    });

    fetchWallets(userId);
  }, [navigate]);

  const fetchWallets = async (userId: string) => {
    try {
      const response = await fetch(`/api/wallets/${userId}`);
      if (!response.ok) {
        throw new Error('Erreur lors du chargement des wallets');
      }
      const data = await response.json();
      setWallets(data);
    } catch (err) {
      console.error(err);
      setError('Impossible de charger vos portefeuilles');
    } finally {
      setLoading(false);
    }
  };

  const totalValue = wallets.reduce((sum, wallet) => {
    const price = prices[wallet.currency] || 0;
    // If currency is EUR/USD, price is 1 (or exchange rate). If crypto, use mock price.
    // Assuming wallet.currency holds the symbol (BTC, EUR, etc.)
    return sum + wallet.balance * price;
  }, 0);

  const handleTransaction = async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;

    try {
      if (transactionForm.type === 'transfer') {
        alert('Les virements ne sont pas encore disponibles via l\'API.');
        return;
      }

      const endpoint = transactionForm.type === 'buy' ? '/transactions/buy' : '/transactions/sell';
      
      // Construct payload based on API requirements
      const payload = transactionForm.type === 'buy' 
        ? {
            userId: parseInt(userId),
            cryptoUnit: transactionForm.crypto,
            amount: parseFloat(transactionForm.amount),
            paymentUnit: 'EUR' // Defaulting to EUR for payment
          }
        : {
            userId: parseInt(userId),
            cryptoUnit: transactionForm.crypto,
            amount: parseFloat(transactionForm.amount),
            targetUnit: 'EUR' // Defaulting to EUR for target
          };

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Erreur lors de la transaction');
      }

      alert(`Transaction réussie : ${data.message || 'Opération en cours'}`);
      
      // Refresh wallets after a short delay to allow backend to process
      setTimeout(() => fetchWallets(userId), 1000);

      setTransactionForm({ 
        crypto: '', 
        currency: 'EUR', 
        amount: '', 
        recipient: '',
        type: 'buy', 
        mode: 'crypto',
        transferType: 'crypto'
      });

    } catch (err: any) {
      alert(`Erreur: ${err.message}`);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      <div className="container mx-auto px-4 py-8">
        {/* Header avec Valeur Totale */}
        <div className="text-center mb-8">
          <h1 className="text-5xl font-bold text-white mb-2">
            ${totalValue.toLocaleString('fr-FR', { minimumFractionDigits: 2 })}
          </h1>
          <p className="text-gray-400 text-sm">Valeur totale estimée du portfolio (EUR)</p>
        </div>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Colonne Gauche - Profil */}
          <div className="lg:col-span-1 space-y-6">
            {/* Carte Profil */}
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-6 border border-white/20">
              <h2 className="text-xl font-bold text-white mb-4">Profil</h2>
              
              <div className="flex flex-col items-center">
                <img 
                  src={userProfile.avatar} 
                  alt="Avatar" 
                  className="w-24 h-24 rounded-full mb-4 border-4 border-purple-500"
                />
                <h3 className="text-xl font-semibold text-white">{userProfile.name}</h3>
                <p className="text-gray-400 text-sm">{userProfile.email}</p>
                <button 
                  onClick={() => {
                    localStorage.clear();
                    navigate('/');
                  }}
                  className="mt-4 text-sm text-red-400 hover:text-red-300 underline"
                >
                  Se déconnecter
                </button>
              </div>
            </div>

            {/* Historique des Transactions */}
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-6 border border-white/20">
              <h2 className="text-xl font-bold text-white mb-4">Historique (Mock)</h2>
              
              <div className="space-y-3 max-h-96 overflow-y-auto scrollbar-custom pr-2">
                <style>{`
                  .scrollbar-custom::-webkit-scrollbar {
                    width: 8px;
                  }
                  .scrollbar-custom::-webkit-scrollbar-track {
                    background: rgba(255, 255, 255, 0.05);
                    border-radius: 10px;
                  }
                  .scrollbar-custom::-webkit-scrollbar-thumb {
                    background: linear-gradient(to bottom, #8b5cf6, #ec4899);
                    border-radius: 10px;
                  }
                  .scrollbar-custom::-webkit-scrollbar-thumb:hover {
                    background: linear-gradient(to bottom, #7c3aed, #db2777);
                  }
                `}</style>
                {transactionHistory.map((tx) => {
                  const getTransactionLabel = () => {
                    if (tx.type === 'buy') return 'Achat';
                    if (tx.type === 'sell') return 'Vente';
                    if (tx.type === 'transfer') return 'Virement';
                  };
                  
                  const getTransactionColor = () => {
                    if (tx.type === 'buy') return 'green';
                    if (tx.type === 'transfer') return 'blue';
                    return 'red';
                  };
                  
                  const color = getTransactionColor();
                  const asset = tx.crypto || tx.currency || '';
                  
                  return (
                    <div 
                      key={tx.id} 
                      className="bg-white/5 rounded-lg p-3 flex items-center justify-between hover:bg-white/10 transition"
                    >
                      <div className="flex items-center space-x-4">
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                          color === 'green' ? 'bg-gradient-to-br from-green-400 to-emerald-500' : 
                          color === 'blue' ? 'bg-gradient-to-br from-blue-400 to-cyan-500' :
                          'bg-gradient-to-br from-red-400 to-rose-500'
                        } shadow-lg`}>
                          <span className={`text-lg font-bold text-white`}>
                            {color === 'green' ? '↑' : color === 'blue' ? '→' : '↓'}
                          </span>
                        </div>
                        <div>
                          <p className="text-white font-semibold text-sm">
                            {getTransactionLabel()} {asset}
                            {tx.recipient && <span className="text-gray-400"> → {tx.recipient}</span>}
                          </p>
                          <p className="text-gray-400 text-xs">{tx.date}</p>
                        </div>
                      </div>
                      <p className="text-white font-semibold text-sm">
                        {tx.crypto ? tx.amount : `${tx.amount} ${tx.currency}`}
                      </p>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Colonne Droite - Wallet et Transactions */}
          <div className="lg:col-span-2 space-y-6">
            {/* Wallet Section */}
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-6 border border-white/20">
              <h2 className="text-2xl font-bold text-white mb-4">Mon Wallet</h2>
              {loading ? (
                <p className="text-white">Chargement...</p>
              ) : error ? (
                <p className="text-red-400">{error}</p>
              ) : (
                <div className="space-y-4">
                  {wallets.length === 0 && <p className="text-gray-400">Aucun portefeuille trouvé.</p>}
                  {wallets.map((wallet) => (
                    <div
                      key={wallet.id}
                      className="bg-white/5 rounded-xl p-4 flex items-center justify-between hover:bg-white/10 transition"
                    >
                      <div className="flex items-center space-x-4">
                        <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-white font-bold">
                          {wallet.currency.substring(0, 2)}
                        </div>
                        <div>
                          <p className="text-white font-semibold">{wallet.currency}</p>
                          <p className="text-gray-400 text-sm">Solde disponible</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="text-white font-semibold">{wallet.balance}</p>
                        <p className="text-gray-400 text-sm">
                          ~ {(wallet.balance * (prices[wallet.currency] || 1)).toLocaleString('fr-FR')} EUR
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Transaction Form */}
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-6 border border-white/20">
              <h2 className="text-2xl font-bold text-white mb-4">
                Nouvelle Transaction
              </h2>
              <div className="space-y-4">
                {/* Type de transaction : Achat/Vente ou Virement */}
                <div>
                  <label className="block text-gray-300 mb-2 text-sm">
                    Type de transaction
                  </label>
                  <div className="grid grid-cols-3 gap-3">
                    <button
                      onClick={() =>
                        setTransactionForm({ ...transactionForm, type: 'buy', mode: 'crypto' })
                      }
                      className={`py-3 rounded-lg font-semibold transition ${
                        transactionForm.type === 'buy'
                          ? 'bg-green-500 text-white'
                          : 'bg-white/5 text-gray-400 hover:bg-white/10'
                      }`}
                    >
                      Acheter
                    </button>
                    <button
                      onClick={() =>
                        setTransactionForm({ ...transactionForm, type: 'sell', mode: 'crypto' })
                      }
                      className={`py-3 rounded-lg font-semibold transition ${
                        transactionForm.type === 'sell'
                          ? 'bg-red-500 text-white'
                          : 'bg-white/5 text-gray-400 hover:bg-white/10'
                      }`}
                    >
                      Vendre
                    </button>
                    <button
                      onClick={() =>
                        setTransactionForm({ ...transactionForm, type: 'transfer' })
                      }
                      className={`py-3 rounded-lg font-semibold transition ${
                        transactionForm.type === 'transfer'
                          ? 'bg-blue-500 text-white'
                          : 'bg-white/5 text-gray-400 hover:bg-white/10'
                      }`}
                    >
                      Virement
                    </button>
                  </div>
                </div>

                {transactionForm.type === 'transfer' ? (
                  // Formulaire de virement
                  <>
                    <div className="p-4 bg-yellow-500/20 border border-yellow-500/50 rounded-lg">
                      <p className="text-yellow-200 text-sm">Les virements ne sont pas encore disponibles.</p>
                    </div>
                  </>
                ) : (
                  // Formulaire Achat/Vente
                  <>
                    <div>
                      <label className="block text-gray-300 mb-2 text-sm">
                        Crypto
                      </label>
                      <select
                        value={transactionForm.crypto}
                        onChange={(e) =>
                          setTransactionForm({
                            ...transactionForm,
                            crypto: e.target.value,
                          })
                        }
                        className="w-full bg-white/5 border border-white/20 rounded-lg px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                        style={{ colorScheme: 'dark' }}
                      >
                        <option value="" style={{ backgroundColor: '#1e293b', color: 'white' }}>Sélectionne une crypto</option>
                        <option value="BTC" style={{ backgroundColor: '#1e293b', color: 'white' }}>Bitcoin (BTC)</option>
                        <option value="ETH" style={{ backgroundColor: '#1e293b', color: 'white' }}>Ethereum (ETH)</option>
                        <option value="SOL" style={{ backgroundColor: '#1e293b', color: 'white' }}>Solana (SOL)</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-gray-300 mb-2 text-sm">
                        Montant
                      </label>
                      <input
                        type="number"
                        step="0.0001"
                        value={transactionForm.amount}
                        onChange={(e) =>
                          setTransactionForm({
                            ...transactionForm,
                            amount: e.target.value,
                          })
                        }
                        placeholder="0.00"
                        className="w-full bg-white/5 border border-white/20 rounded-lg px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                      />
                    </div>
                  </>
                )}

                <button
                  onClick={handleTransaction}
                  className="w-full bg-gradient-to-r from-purple-500 to-pink-500 text-white font-semibold py-3 rounded-lg hover:from-purple-600 hover:to-pink-600 transition"
                >
                  Confirmer la transaction
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
