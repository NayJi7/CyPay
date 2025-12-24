import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Swal from 'sweetalert2';

// -- TYPES --
interface Wallet {
  id: number;
  userId: number;
  currency: string;
  balance: number;
}

interface Transaction {
  id: number;
  type: 'BUY' | 'SELL' | 'TRANSFER' | 'ORDER';
  actor1: number;
  actor2?: number;
  amount: number;
  unit: string;
  timestamp: string;
  status: string;
}

// NOUVEAU : Interface pour CoinGecko
interface MarketData {
  bitcoin: { eur: number; usd: number; eur_24h_change: number };
  ethereum: { eur: number; usd: number; eur_24h_change: number };
}

// Configuration SweetAlert2 Theme
const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true,
  background: '#1e293b',
  color: '#e2e8f0',
  didOpen: (toast) => {
    toast.addEventListener('mouseenter', Swal.stopTimer)
    toast.addEventListener('mouseleave', Swal.resumeTimer)
  }
});

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

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [wallets, setWallets] = useState<Wallet[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // NOUVEAU : État pour les données CoinGecko
  const [marketData, setMarketData] = useState<MarketData | null>(null);

  // Mock prices (interne pour le calcul du total si l'API échoue ou pour les autres devises)
  const [prices, setPrices] = useState<Record<string, number>>({
    BTC: 45000,
    ETH: 3000,
    EUR: 1,
    USD: 0.92,
  });

  const [transactionForm, setTransactionForm] = useState({
    crypto: '',
    currency: 'EUR',
    amount: '',
    recipientId: '',
    type: 'BUY' as 'BUY' | 'SELL' | 'TRANSFER',
  });

  // NOUVEAU: État pour le montant en Fiat (synchro avec montant crypto)
  const [fiatAmount, setFiatAmount] = useState('');
  
  // NOUVEAU: Préférence d'affichage devise (EUR/USD)
  const [preferredCurrency, setPreferredCurrency] = useState<'EUR' | 'USD'>('EUR');

  const [userProfile, setUserProfile] = useState({
    name: '',
    email: '',
    avatar: ''
  });

  // -- HELPERS --
  const getCurrentPrice = (cryptoSym: string, fiatSym: string) => {
    if (!cryptoSym) return 0;
    
    // 1. Essayer CoinGecko
    if (marketData) {
      const c = cryptoSym === 'BTC' ? 'bitcoin' : 'ethereum';
      const f = fiatSym.toLowerCase();
      // @ts-ignore
      if (marketData[c] && marketData[c][f]) return marketData[c][f];
    }

    // 2. Fallback prix internes
    const key = `${cryptoSym}_${fiatSym}`; // ex: BTC_EUR
    if (prices[key]) return prices[key];

    // 3. Fallback conversion via EUR
    const cryptoEur = prices[cryptoSym] || prices[`${cryptoSym}_EUR`] || 0;
    const fiatEur = fiatSym === 'EUR' ? 1 : (prices[fiatSym] || 0.92); // approx
    if (fiatEur === 0) return 0;
    
    return cryptoEur / fiatEur;
  };

  const handleCryptoChange = (val: string) => {
    setTransactionForm({ ...transactionForm, amount: val });
    
    if (!val || isNaN(parseFloat(val))) {
      setFiatAmount('');
      return;
    }
    
    const price = getCurrentPrice(transactionForm.crypto, transactionForm.currency);
    if (price > 0) {
      const fiat = parseFloat(val) * price;
      setFiatAmount(fiat.toFixed(2));
    }
  };

  const handleFiatChange = (val: string) => {
    setFiatAmount(val);

    if (!val || isNaN(parseFloat(val))) {
      setTransactionForm({ ...transactionForm, amount: '' });
      return;
    }

    const price = getCurrentPrice(transactionForm.crypto, transactionForm.currency);
    if (price > 0) {
      const crypto = parseFloat(val) / price;
      setTransactionForm({ ...transactionForm, amount: crypto.toFixed(8) });
    }
  };

  // Met à jour les conversions si on change de devise ou de crypto
  useEffect(() => {
    if (transactionForm.amount && !fiatAmount) {
       handleCryptoChange(transactionForm.amount);
    }
  }, [transactionForm.crypto, transactionForm.currency]);


  // -- LOGIC --
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
      avatar: `https://ui-avatars.com/api/?name=${userPseudo || 'User'}&background=0ea5e9&color=fff&size=128&bold=true`
    });

    fetchWallets(userId);
    fetchHistory(userId);
    fetchInternalPrices();
    
    // NOUVEAU : Appel initial CoinGecko
    fetchCoinGeckoData();

    // Refresh CoinGecko toutes les 60s (Rate limit friendly)
    const marketInterval = setInterval(fetchCoinGeckoData, 60000);
    const internalInterval = setInterval(fetchInternalPrices, 30000);

    return () => {
      clearInterval(marketInterval);
      clearInterval(internalInterval);
    };
  }, [navigate]);

  // NOUVEAU : Fonction fetch CoinGecko
  const fetchCoinGeckoData = async () => {
    try {
      const response = await fetch('https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=eur,usd&include_24hr_change=true');
      if (response.ok) {
        const data = await response.json();
        setMarketData(data);
        
        // On met à jour nos prix internes pour que le calcul du "Total Portfolio" soit précis
        setPrices(prev => ({
          ...prev,
          BTC: data.bitcoin.eur,
          ETH: data.ethereum.eur
        }));
      }
    } catch (e) {
      console.error("Erreur CoinGecko (Mode offline activé)", e);
    }
  };

  const fetchInternalPrices = async () => {
    try {
      const response = await fetch('/transactions/prices');
      if (response.ok) {
        const data = await response.json();
        // Fallback si CoinGecko échoue
        if (!marketData) {
            const newPrices: Record<string, number> = { ...data };
            if (data.BTC_EUR) newPrices.BTC = data.BTC_EUR;
            if (data.ETH_EUR) newPrices.ETH = data.ETH_EUR;
            newPrices.EUR = 1;
            setPrices(newPrices);
        }
      }
    } catch (e) {
      console.error(e);
    }
  };

  const fetchWallets = async (userId: string) => {
    try {
      const response = await fetch(`/api/wallets/${userId}`);
      if (!response.ok) throw new Error('Erreur chargement wallets');
      const data = await response.json();
      setWallets(data);
    } catch (err) {
      console.error(err);
      setError('Impossible de charger vos portefeuilles');
    } finally {
      setLoading(false);
    }
  };

  const fetchHistory = async (userId: string) => {
    try {
      const response = await fetch(`/transactions/history/${userId}`);
      if (!response.ok) throw new Error('Erreur chargement historique');
      const data = await response.json();
      setTransactions(data);
    } catch (err) {
      console.error(err);
    }
  };

  const createWallet = async (currency: string) => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;

    try {
      const response = await fetch('/api/wallets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: parseInt(userId), currency })
      });
      if (!response.ok) throw new Error('Erreur création wallet');
      
      swalDark.fire({
        icon: 'success',
        title: 'Wallet Créé',
        text: `Votre portefeuille ${currency} est prêt !`,
        timer: 2000,
        showConfirmButton: false
      });
      fetchWallets(userId);
    } catch (err: any) {
      swalDark.fire({
        icon: 'error',
        title: 'Échec',
        text: err.message
      });
    }
  };

  const [walletToDelete, setWalletToDelete] = useState<Wallet | null>(null);
  const [transferTargetId, setTransferTargetId] = useState<string>('');

  const performDelete = async (walletId: number) => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;
    try {
      const response = await fetch(`/api/wallets/${walletId}`, { method: 'DELETE' });
      if (!response.ok) throw new Error('Erreur suppression wallet');
      
      Toast.fire({
        icon: 'success',
        title: 'Wallet supprimé avec succès'
      });
      fetchWallets(userId);
      setWalletToDelete(null);
    } catch (err: any) {
      swalDark.fire({
        icon: 'error',
        title: 'Erreur',
        text: err.message
      });
    }
  };

  const handleTransferAndDelete = async () => {
    if (!walletToDelete || !transferTargetId) return;
    const targetWallet = wallets.find(w => w.id.toString() === transferTargetId);
    if (!targetWallet) return;

    const userId = localStorage.getItem('userId');
    if (!userId) return;

    try {
      let endpoint = '';
      let payload: any = {};
      
      const isSourceCrypto = ['BTC', 'ETH'].includes(walletToDelete.currency);
      const isTargetFiat = ['EUR', 'USD'].includes(targetWallet.currency);
      const isSourceFiat = ['EUR', 'USD'].includes(walletToDelete.currency);
      const isTargetCrypto = ['BTC', 'ETH'].includes(targetWallet.currency);

      if (isSourceCrypto && isTargetFiat) {
          endpoint = '/transactions/sell';
          payload = {
              userId: parseInt(userId),
              cryptoUnit: walletToDelete.currency,
              amount: walletToDelete.balance,
              targetUnit: targetWallet.currency
          };
      } else if (isSourceFiat && isTargetCrypto) {
          const pairKey = `${targetWallet.currency}_${walletToDelete.currency}`;
          let price = prices[pairKey];
          if (!price) {
             const sourcePrice = prices[walletToDelete.currency] || 1;
             const targetPrice = prices[targetWallet.currency] || 1;
             price = targetPrice / sourcePrice;
          }
          const amountCrypto = walletToDelete.balance / price;

          endpoint = '/transactions/buy';
          payload = {
              userId: parseInt(userId),
              cryptoUnit: targetWallet.currency,
              amount: amountCrypto,
              paymentUnit: walletToDelete.currency
          };
      } else {
          swalDark.fire({
            icon: 'warning',
            title: 'Action Impossible',
            text: "Transfert direct impossible. Devises incompatibles."
          });
          return;
      }

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.message || data.error || 'Erreur lors du transfert');
      }

      await performDelete(walletToDelete.id);

    } catch (err: any) {
      swalDark.fire({
        icon: 'error',
        title: 'Erreur Transfert',
        text: err.message
      });
    }
  };

  const deleteWallet = async (walletId: number) => {
    if (wallets.length <= 1) {
      swalDark.fire({
        icon: 'warning',
        title: 'Attention',
        text: "Impossible de supprimer votre dernier wallet !"
      });
      return;
    }

    const wallet = wallets.find(w => w.id === walletId);
    if (!wallet) return;

    if (wallet.balance > 0) {
      setWalletToDelete(wallet);
      setTransferTargetId('');
      return;
    }

    const result = await swalDark.fire({
      title: 'Confirmer la suppression ?',
      text: "Cette action est irréversible.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Oui, supprimer',
      cancelButtonText: 'Annuler'
    });

    if (result.isConfirmed) {
      performDelete(walletId);
    }
  };

  const totalValueEur = wallets.reduce((sum, wallet) => {
    const price = prices[wallet.currency] || 0;
    return sum + wallet.balance * price;
  }, 0);

  const totalValue = preferredCurrency === 'EUR' 
    ? totalValueEur 
    : totalValueEur / (prices['USD'] || 0.92);

  const handleTransaction = async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;

    try {
      let endpoint = '';
      let payload: any = {};

      if (transactionForm.type === 'TRANSFER') {
        endpoint = '/transactions/transfer';
        payload = {
          fromUserId: parseInt(userId),
          toUserId: parseInt(transactionForm.recipientId),
          cryptoUnit: transactionForm.crypto,
          amount: parseFloat(transactionForm.amount)
        };
      } else {
        endpoint = transactionForm.type === 'BUY' ? '/transactions/buy' : '/transactions/sell';
        payload = transactionForm.type === 'BUY'
          ? {
              userId: parseInt(userId),
              cryptoUnit: transactionForm.crypto,
              amount: parseFloat(transactionForm.amount),
              paymentUnit: transactionForm.currency // Utilisation dynamique de la devise
            }
          : {
              userId: parseInt(userId),
              cryptoUnit: transactionForm.crypto,
              amount: parseFloat(transactionForm.amount),
              targetUnit: transactionForm.currency // Utilisation dynamique de la devise cible
            };
      }

      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await response.json();

      if (!response.ok) {
        // Backend returns ErrorResponse { error: string } on error
        throw new Error(data.message || data.error || 'Erreur lors de la transaction');
      }

      swalDark.fire({
        icon: 'success',
        title: 'Transaction Initiée',
        text: data.message || 'Votre demande a été envoyée au réseau.',
        timer: 3000
      });
      
      // Refresh Strategy: Immediate + Polling for eventual consistency
      const refreshData = () => {
        fetchWallets(userId);
        fetchHistory(userId);
      };

      refreshData(); // Immediate
      
      // Polling every 500ms for 2.5 seconds to catch async updates
      let attempts = 0;
      const interval = setInterval(() => {
          refreshData();
          attempts++;
          if (attempts >= 5) clearInterval(interval);
      }, 500);

      setTransactionForm({ 
        crypto: '', 
        currency: 'EUR', 
        amount: '', 
        recipientId: '', 
        type: 'BUY'
      });
      setFiatAmount('');

    } catch (err: any) {
      swalDark.fire({
        icon: 'error',
        title: 'Échec Transaction',
        text: err.message
      });
    }
  };

  // -- RENDER --
  return (
    <div className="min-h-screen bg-[#050b14] text-slate-300 font-sans selection:bg-cyan-500/30">
      
      {/* Top Navbar */}
      <nav className="border-b border-white/10 bg-[#050b14]/90 backdrop-blur sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
          <div className="flex items-center space-x-3">
             <div className="w-12 h-12 bg-gradient-to-tr from-cyan-500 to-blue-600 rounded-full flex items-center justify-center shadow-[0_0_15px_rgba(6,182,212,0.5)] overflow-hidden">
               <img src="/icon.jpg" alt="Logo" className="w-full h-full object-cover" />
             </div>
             <div>
               <h1 className="text-xl font-bold text-white tracking-tight">DASHBOARD <span className="text-cyan-500 font-mono text-xs opacity-80">v2.4</span></h1>
             </div>
          </div>
          
          <div className="flex items-center space-x-6">
            <div className="hidden md:flex items-center space-x-4 bg-white/5 px-4 py-2 rounded-full border border-white/5">
              <img src={userProfile.avatar} alt="User" className="w-8 h-8 rounded-full border border-cyan-500/50" />
              <div className="text-sm">
                <p className="text-white font-medium leading-none">{userProfile.name}</p>
                <p className="text-xs text-slate-500 font-mono">{userProfile.email}</p>
              </div>
            </div>
            <button 
              onClick={() => { localStorage.clear(); navigate('/'); }}
              className="p-2 text-slate-400 hover:text-red-400 transition-colors"
              title="Déconnexion"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </button>
          </div>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto px-6 py-8">
        
        {/* Total Balance Hero */}
        <div className="mb-12 relative group">
          <div className="absolute inset-0 bg-gradient-to-r from-cyan-500/10 to-purple-500/10 rounded-3xl blur-3xl group-hover:blur-[100px] transition-all duration-1000"></div>
          <div className="relative bg-white/5 border border-white/10 rounded-3xl p-8 backdrop-blur-sm flex flex-col md:flex-row justify-between items-center">
            <div>
              <div className="flex items-center gap-3 mb-1">
                <p className="text-cyan-400 font-mono text-sm uppercase tracking-widest">Portefeuille Global</p>
                <button 
                  onClick={() => setPreferredCurrency(c => c === 'EUR' ? 'USD' : 'EUR')}
                  className="px-2 py-0.5 rounded border border-cyan-500/30 text-[10px] font-bold text-cyan-500 hover:bg-cyan-500/10 transition"
                >
                  {preferredCurrency} ⇄
                </button>
              </div>
              <h2 className="text-6xl font-black text-white tracking-tighter drop-shadow-[0_0_10px_rgba(255,255,255,0.3)]">
                {totalValue.toLocaleString(preferredCurrency === 'EUR' ? 'fr-FR' : 'en-US', { style: 'currency', currency: preferredCurrency })}
              </h2>
            </div>
            <div className="mt-6 md:mt-0 flex gap-2">
               {['BTC', 'ETH', 'EUR', 'USD'].map(c => (
                 <button 
                  key={c}
                  onClick={() => createWallet(c)}
                  className="px-4 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-600 hover:border-cyan-500/50 text-xs font-bold text-slate-300 transition-all active:scale-95"
                 >
                   + {c}
                 </button>
               ))}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* LEFT COLUMN: Operations & History (lg:col-span-8) */}
          <div className="lg:col-span-8 space-y-8">
            
            {/* Wallets Grid */}
            <div>
              <h3 className="text-lg font-bold text-white mb-4 flex items-center">
                <svg className="w-5 h-5 mr-2 text-cyan-500" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>
                MES PORTEFEUILLES
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {wallets.map((wallet) => {
                  const isCrypto = ['BTC', 'ETH'].includes(wallet.currency);
                  const priceInEur = prices[wallet.currency] || 1;
                  const value = preferredCurrency === 'EUR' 
                    ? wallet.balance * priceInEur
                    : (wallet.balance * priceInEur) / (prices['USD'] || 0.92);

                  return (
                    <div key={wallet.id} className="group relative bg-[#0f172a] rounded-2xl p-6 border border-slate-800 hover:border-cyan-500/30 transition-all overflow-hidden">
                      <div className={`absolute top-0 right-0 p-8 rounded-bl-[100px] opacity-10 transition-opacity group-hover:opacity-20 ${isCrypto ? 'bg-cyan-500' : 'bg-purple-500'}`}></div>
                      
                      <div className="relative z-10">
                        <div className="flex justify-between items-start mb-4">
                          <span className={`text-3xl font-bold ${isCrypto ? 'text-cyan-400' : 'text-purple-400'}`}>{wallet.currency}</span>
                          <button onClick={() => deleteWallet(wallet.id)} className="text-slate-600 hover:text-red-500 transition-colors">✕</button>
                        </div>
                        
                        <div className="space-y-1">
                          <p className="text-3xl font-mono text-white tracking-tight">{wallet.balance}</p>
                          <p className="text-xs text-slate-500 font-mono">
                             ≈ {value.toLocaleString(preferredCurrency === 'EUR' ? 'fr-FR' : 'en-US', { style: 'currency', currency: preferredCurrency })}
                          </p>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Transaction Terminal */}
            <div className="bg-[#0a0a0a] rounded-2xl border border-slate-800 overflow-hidden shadow-2xl">
              <div className="bg-[#111] px-6 py-3 border-b border-slate-800 flex justify-between items-center">
                <h3 className="text-sm font-mono text-cyan-500 flex items-center">
                  <span className="w-2 h-2 bg-green-500 rounded-full mr-2 animate-pulse"></span>
                  LIVE_FEED_TRANSACTIONS
                </h3>
              </div>
              <div className="h-[400px] overflow-y-auto p-4 font-mono text-sm scrollbar-thin scrollbar-thumb-slate-700 scrollbar-track-transparent">
                <table className="w-full text-left border-collapse">
                  <thead className="text-xs text-slate-500 uppercase bg-[#111] sticky top-0">
                    <tr>
                      <th className="py-2 pl-2">Type</th>
                      <th className="py-2">Amount</th>
                      <th className="py-2">Date</th>
                      <th className="py-2 text-right pr-2">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/50">
                    {transactions.length === 0 ? (
                      <tr><td colSpan={4} className="py-8 text-center text-slate-600">-- NO DATA SIGNAL --</td></tr>
                    ) : (
                      transactions.map((tx) => {
                         const colorClass = tx.type === 'BUY' ? 'text-green-400' : tx.type === 'SELL' ? 'text-red-400' : 'text-blue-400';
                         const symbol = tx.type === 'BUY' ? '▼' : tx.type === 'SELL' ? '▲' : '►';
                         return (
                          <tr key={tx.id} className="hover:bg-white/5 transition-colors">
                            <td className={`py-3 pl-2 font-bold ${colorClass}`}>{symbol} {tx.type}</td>
                            <td className="py-3 text-slate-300">{tx.amount} <span className="text-slate-500 text-xs">{tx.unit}</span></td>
                            <td className="py-3 text-slate-500 text-xs">{new Date(tx.timestamp).toLocaleDateString()} {new Date(tx.timestamp).toLocaleTimeString()}</td>
                            <td className="py-3 text-right pr-2">
                              <span className={`px-2 py-0.5 rounded text-[10px] border ${tx.status === 'SUCCESS' ? 'border-green-900 text-green-500 bg-green-900/10' : 'border-yellow-900 text-yellow-500'}`}>
                                {tx.status}
                              </span>
                            </td>
                          </tr>
                         );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* RIGHT COLUMN: Control Panel & Market Data (lg:col-span-4) */}
          <div className="lg:col-span-4 space-y-8">
            {/* Control Panel */}
            <div className="bg-slate-900/50 backdrop-blur-xl border border-white/10 rounded-3xl p-6 shadow-2xl">
              <h3 className="text-xl font-bold text-white mb-6 border-b border-white/10 pb-4">PANNEAU DE CONTRÔLE</h3>
              
              <div className="grid grid-cols-3 gap-2 mb-6 p-1 bg-black/40 rounded-xl">
                {['BUY', 'SELL', 'TRANSFER'].map((t) => (
                  <button
                    key={t}
                    onClick={() => setTransactionForm({ ...transactionForm, type: t as any })}
                    className={`py-2 text-xs font-bold rounded-lg transition-all ${
                      transactionForm.type === t
                        ? 'bg-cyan-600 text-white shadow-lg shadow-cyan-500/20'
                        : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'
                    }`}
                  >
                    {t}
                  </button>
                ))}
              </div>

              <div className="space-y-4">
                
                {/* Asset Selection */}
                <div>
                  <label className="text-xs font-mono text-cyan-500 mb-1 block">ASSET_TYPE</label>
                  <select
                    value={transactionForm.crypto}
                    onChange={(e) => setTransactionForm({ ...transactionForm, crypto: e.target.value })}
                    className="w-full bg-[#050b14] border border-slate-700 rounded-lg p-3 text-white focus:border-cyan-500 outline-none"
                  >
                    <option value="">Sélectionner...</option>
                    <option value="BTC">Bitcoin (BTC)</option>
                    <option value="ETH">Ethereum (ETH)</option>
                  </select>
                </div>

                {/* Currency Selection (Only for BUY/SELL) */}
                {transactionForm.type !== 'TRANSFER' && (
                  <div>
                    <label className="text-xs font-mono text-cyan-500 mb-1 block">PAYMENT_CURRENCY</label>
                    <div className="flex bg-[#050b14] border border-slate-700 rounded-lg p-1">
                      {['EUR', 'USD'].map((curr) => (
                        <button
                          key={curr}
                          onClick={() => setTransactionForm({ ...transactionForm, currency: curr })}
                          className={`flex-1 py-2 text-xs font-bold rounded-md transition-all ${
                            transactionForm.currency === curr
                              ? 'bg-slate-700 text-white shadow-sm'
                              : 'text-slate-500 hover:text-slate-300'
                          }`}
                        >
                          {curr}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* Amount Inputs */}
                <div className="space-y-3">
                   {/* Crypto Amount */}
                   <div>
                      <label className="text-xs font-mono text-cyan-500 mb-1 block flex justify-between">
                        <span>QUANTITY ({transactionForm.crypto || 'CRYPTO'})</span>
                      </label>
                      <input
                        type="number"
                        step="0.000001"
                        value={transactionForm.amount}
                        onChange={(e) => handleCryptoChange(e.target.value)}
                        placeholder="0.00"
                        disabled={!transactionForm.crypto}
                        className="w-full bg-[#050b14] border border-slate-700 rounded-lg p-3 text-white focus:border-cyan-500 outline-none font-mono disabled:opacity-50"
                      />
                   </div>

                   {/* Fiat Amount (Only for BUY/SELL) */}
                   {transactionForm.type !== 'TRANSFER' && (
                     <div className="relative">
                        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                          <span className="text-slate-700 text-xs font-mono">≈</span>
                        </div>
                        <label className="text-xs font-mono text-cyan-500 mb-1 block">
                          AMOUNT ({transactionForm.currency})
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          value={fiatAmount}
                          onChange={(e) => handleFiatChange(e.target.value)}
                          placeholder="0.00"
                          disabled={!transactionForm.crypto}
                          className="w-full bg-[#050b14] border border-slate-700 rounded-lg p-3 text-white focus:border-cyan-500 outline-none font-mono disabled:opacity-50"
                        />
                     </div>
                   )}
                </div>

                {transactionForm.type === 'TRANSFER' && (
                  <div>
                    <label className="text-xs font-mono text-cyan-500 mb-1 block">TARGET_ID</label>
                    <input
                      type="number"
                      value={transactionForm.recipientId}
                      onChange={(e) => setTransactionForm({ ...transactionForm, recipientId: e.target.value })}
                      placeholder="ID Utilisateur"
                      className="w-full bg-[#050b14] border border-slate-700 rounded-lg p-3 text-white focus:border-cyan-500 outline-none font-mono"
                    />
                  </div>
                )}

                <button
                  onClick={handleTransaction}
                  className="w-full mt-6 bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 text-white font-bold py-4 rounded-xl shadow-lg shadow-cyan-900/20 transition-all border border-cyan-400/20"
                >
                  VALIDER
                </button>
              </div>
            </div>

            {/* NOUVEAU: MARKET DATA WIDGET (COINGECKO) */}
            <div className="bg-[#050b14] border border-slate-800 rounded-3xl p-6 relative overflow-hidden group">
              {/* Background ambient glow */}
              <div className="absolute top-0 right-0 w-32 h-32 bg-cyan-500/10 blur-[50px] rounded-full group-hover:bg-cyan-500/20 transition-all"></div>
              
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-sm font-mono text-slate-500 flex items-center gap-2">
                  <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse shadow-[0_0_10px_#22c55e]"></span>
                  COURS DU MARCHÉ
                </h3>
                <span className="text-[10px] text-slate-600 border border-slate-800 px-2 py-1 rounded">COINGECKO API</span>
              </div>

              {!marketData ? (
                 <div className="animate-pulse space-y-4">
                    <div className="h-16 bg-slate-800/50 rounded-xl"></div>
                    <div className="h-16 bg-slate-800/50 rounded-xl"></div>
                 </div>
              ) : (
                <div className="space-y-3">
                  {/* BITCOIN CARD */}
                  <div className="bg-slate-900/50 border border-white/5 rounded-xl p-4 flex items-center justify-between hover:border-orange-500/30 transition-all">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-orange-500/20 flex items-center justify-center text-orange-500 font-bold">₿</div>
                      <div>
                        <div className="text-white font-bold text-sm">Bitcoin</div>
                        <div className="text-slate-500 text-xs">BTC/{preferredCurrency}</div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-white font-mono font-bold">
                        {marketData.bitcoin[preferredCurrency.toLowerCase() as 'eur' | 'usd'].toLocaleString(preferredCurrency === 'EUR' ? 'fr-FR' : 'en-US', { style: 'currency', currency: preferredCurrency })}
                      </div>
                      <div className={`text-xs font-mono ${marketData.bitcoin.eur_24h_change >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                        {marketData.bitcoin.eur_24h_change >= 0 ? '+' : ''}{marketData.bitcoin.eur_24h_change.toFixed(2)}%
                      </div>
                    </div>
                  </div>

                  {/* ETHEREUM CARD */}
                  <div className="bg-slate-900/50 border border-white/5 rounded-xl p-4 flex items-center justify-between hover:border-purple-500/30 transition-all">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-purple-500/20 flex items-center justify-center text-purple-400 font-bold">Ξ</div>
                      <div>
                        <div className="text-white font-bold text-sm">Ethereum</div>
                        <div className="text-slate-500 text-xs">ETH/{preferredCurrency}</div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-white font-mono font-bold">
                        {marketData.ethereum[preferredCurrency.toLowerCase() as 'eur' | 'usd'].toLocaleString(preferredCurrency === 'EUR' ? 'fr-FR' : 'en-US', { style: 'currency', currency: preferredCurrency })}
                      </div>
                      <div className={`text-xs font-mono ${marketData.ethereum.eur_24h_change >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                         {marketData.ethereum.eur_24h_change >= 0 ? '+' : ''}{marketData.ethereum.eur_24h_change.toFixed(2)}%
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>

          </div>
        </div>
      </div>

      {/* MODAL TRANSFERT */}
      {walletToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 backdrop-blur-sm p-4">
          <div className="bg-slate-900 border border-red-500/30 rounded-2xl p-8 max-w-md w-full relative shadow-[0_0_50px_rgba(220,38,38,0.2)]">
            <h3 className="text-xl font-bold text-white mb-2 flex items-center text-red-400">
              <svg className="w-6 h-6 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
              FONDS RESTANTS DÉTECTÉS
            </h3>
            <p className="text-slate-400 mb-6 text-sm">
              Le wallet <span className="font-bold text-white">{walletToDelete.currency}</span> contient encore <span className="font-mono text-cyan-400">{walletToDelete.balance}</span>.
              <br/>Transférez les fonds avant la clôture du compte.
            </p>

            <select
              value={transferTargetId}
              onChange={(e) => setTransferTargetId(e.target.value)}
              className="w-full bg-black border border-slate-700 rounded-lg p-3 text-white mb-6 focus:border-red-500 outline-none"
            >
              <option value="">Choisir wallet de destination...</option>
              {wallets.filter(w => w.id !== walletToDelete.id).map(w => (
                <option key={w.id} value={w.id}>{w.currency} (Solde: {w.balance})</option>
              ))}
            </select>

            <div className="flex gap-3">
              <button onClick={() => setWalletToDelete(null)} className="flex-1 py-3 rounded-lg border border-slate-700 text-slate-400 hover:bg-slate-800 transition">Annuler</button>
              <button onClick={handleTransferAndDelete} disabled={!transferTargetId} className="flex-1 py-3 rounded-lg bg-red-600 hover:bg-red-500 text-white font-bold disabled:opacity-50">Transférer & Clôturer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;