import React, { useState } from 'react';

interface Crypto {
  id: string;
  symbol: string;
  name: string;
  balance: number;
  price: number;
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
  const [cryptos, setCryptos] = useState<Crypto[]>([
    { id: '1', symbol: 'BTC', name: 'Bitcoin', balance: 0.5, price: 45000 },
    { id: '2', symbol: 'ETH', name: 'Ethereum', balance: 2.3, price: 3000 },
    { id: '3', symbol: 'SOL', name: 'Solana', balance: 10, price: 100 },
  ]);

  const [transactionForm, setTransactionForm] = useState({
    crypto: '',
    currency: 'EUR',
    amount: '',
    recipient: '',
    type: 'buy' as 'buy' | 'sell' | 'transfer',
    mode: 'crypto' as 'crypto' | 'fiat',
    transferType: 'crypto' as 'crypto' | 'fiat',
  });

  const [userProfile] = useState({
    name: 'Jean Dupont',
    email: 'jean.dupont@email.com',
    avatar: 'https://ui-avatars.com/api/?name=Jean+Dupont&background=8b5cf6&color=fff&size=128'
  });

  const [transactionHistory] = useState<Transaction[]>([
    { id: '1', type: 'buy', crypto: 'BTC', amount: 0.2, date: '2024-12-05' },
    { id: '2', type: 'transfer', currency: 'EUR', amount: 500, recipient: 'alice@email.com', date: '2024-12-05' },
    { id: '3', type: 'sell', crypto: 'ETH', amount: 0.5, date: '2024-12-04' },
    { id: '4', type: 'transfer', crypto: 'BTC', amount: 0.1, recipient: 'bob@email.com', date: '2024-12-04' },
    { id: '5', type: 'buy', crypto: 'SOL', amount: 5, date: '2024-12-03' },
    { id: '6', type: 'buy', crypto: 'BTC', amount: 0.3, date: '2024-12-02' },
  ]);

  const totalValue = cryptos.reduce(
    (sum, crypto) => sum + crypto.balance * crypto.price,
    0
  );

  const handleTransaction = () => {
    console.log('Transaction:', transactionForm);
    
    if (transactionForm.type === 'transfer') {
      const asset = transactionForm.transferType === 'crypto' ? transactionForm.crypto : transactionForm.currency;
      alert(`Virement de ${transactionForm.amount} ${asset} vers ${transactionForm.recipient} envoyé !`);
    } else {
      const asset = transactionForm.mode === 'crypto' ? transactionForm.crypto : transactionForm.currency;
      const typeLabel = transactionForm.type === 'buy' ? 'Achat' : 'Vente';
      alert(`${typeLabel} de ${transactionForm.amount} ${asset} envoyé !`);
    }
    
    setTransactionForm({ 
      crypto: '', 
      currency: 'EUR', 
      amount: '', 
      recipient: '',
      type: 'buy', 
      mode: 'crypto',
      transferType: 'crypto'
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      <div className="container mx-auto px-4 py-8">
        {/* Header avec Valeur Totale */}
        <div className="text-center mb-8">
          <h1 className="text-5xl font-bold text-white mb-2">
            ${totalValue.toLocaleString('fr-FR', { minimumFractionDigits: 2 })}
          </h1>
          <p className="text-gray-400 text-sm">Valeur totale du portfolio</p>
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
              </div>
            </div>

            {/* Historique des Transactions */}
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-6 border border-white/20">
              <h2 className="text-xl font-bold text-white mb-4">Historique</h2>
              
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
              <div className="space-y-4">
                {cryptos.map((crypto) => (
                  <div
                    key={crypto.id}
                    className="bg-white/5 rounded-xl p-4 flex items-center justify-between hover:bg-white/10 transition"
                  >
                    <div className="flex items-center space-x-4">
                      <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-pink-500 rounded-full flex items-center justify-center text-white font-bold">
                        {crypto.symbol.substring(0, 2)}
                      </div>
                      <div>
                        <p className="text-white font-semibold">{crypto.name}</p>
                        <p className="text-gray-400 text-sm">{crypto.symbol}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="text-white font-semibold">{crypto.balance}</p>
                      <p className="text-gray-400 text-sm">
                        ${(crypto.balance * crypto.price).toLocaleString('fr-FR')}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
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
                    <div>
                      <label className="block text-gray-300 mb-2 text-sm">
                        Type d'actif à virer
                      </label>
                      <div className="flex gap-4">
                        <button
                          onClick={() =>
                            setTransactionForm({ ...transactionForm, transferType: 'crypto' })
                          }
                          className={`flex-1 py-3 rounded-lg font-semibold transition ${
                            transactionForm.transferType === 'crypto'
                              ? 'bg-purple-500 text-white'
                              : 'bg-white/5 text-gray-400 hover:bg-white/10'
                          }`}
                        >
                          Crypto
                        </button>
                        <button
                          onClick={() =>
                            setTransactionForm({ ...transactionForm, transferType: 'fiat' })
                          }
                          className={`flex-1 py-3 rounded-lg font-semibold transition ${
                            transactionForm.transferType === 'fiat'
                              ? 'bg-purple-500 text-white'
                              : 'bg-white/5 text-gray-400 hover:bg-white/10'
                          }`}
                        >
                          Monnaie
                        </button>
                      </div>
                    </div>

                    {transactionForm.transferType === 'crypto' ? (
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
                          {cryptos.map((crypto) => (
                            <option key={crypto.id} value={crypto.symbol} style={{ backgroundColor: '#1e293b', color: 'white' }}>
                              {crypto.name} ({crypto.symbol})
                            </option>
                          ))}
                        </select>
                      </div>
                    ) : (
                      <div>
                        <label className="block text-gray-300 mb-2 text-sm">
                          Monnaie
                        </label>
                        <select
                          value={transactionForm.currency}
                          onChange={(e) =>
                            setTransactionForm({
                              ...transactionForm,
                              currency: e.target.value,
                            })
                          }
                          className="w-full bg-white/5 border border-white/20 rounded-lg px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                          style={{ colorScheme: 'dark' }}
                        >
                          <option value="EUR" style={{ backgroundColor: '#1e293b', color: 'white' }}>Euro (EUR)</option>
                          <option value="USD" style={{ backgroundColor: '#1e293b', color: 'white' }}>Dollar (USD)</option>
                          <option value="GBP" style={{ backgroundColor: '#1e293b', color: 'white' }}>Livre Sterling (GBP)</option>
                          <option value="CHF" style={{ backgroundColor: '#1e293b', color: 'white' }}>Franc Suisse (CHF)</option>
                        </select>
                      </div>
                    )}

                    <div>
                      <label className="block text-gray-300 mb-2 text-sm">
                        Bénéficiaire
                      </label>
                      <input
                        type="text"
                        value={transactionForm.recipient}
                        onChange={(e) =>
                          setTransactionForm({
                            ...transactionForm,
                            recipient: e.target.value,
                          })
                        }
                        placeholder="email@exemple.com ou adresse wallet"
                        className="w-full bg-white/5 border border-white/20 rounded-lg px-4 py-3 text-white focus:outline-none focus:ring-2 focus:ring-purple-500"
                      />
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
                        {cryptos.map((crypto) => (
                          <option key={crypto.id} value={crypto.symbol} style={{ backgroundColor: '#1e293b', color: 'white' }}>
                            {crypto.name} ({crypto.symbol})
                          </option>
                        ))}
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