# 7S7C_Ore_Alert - Système avancé de détection de minage suspect

Un plugin Spigot spécialement conçu pour les serveurs 7S7C qui surveille le minage des blocs et alerte les administrateurs en cas de minage suspect de minerais précieux avec des seuils personnalisés par type de minerai.

## 🚀 Fonctionnalités

- Détection intelligente du minage de tous les types de minerais
- Seuils personnalisables pour chaque type de minerai (seuil suspect, très suspect et maximum réaliste)
- Alertes en temps réel aux administrateurs avec différents niveaux de gravité
- Bannissement automatique en cas d'activité très suspecte
- Configuration complète via fichiers YAML
- Système de permissions avancé
- Support multilingue des messages


## ⚙️ Configuration

Le plugin crée automatiquement un fichier `config.yml` avec des paramètres par défaut optimisés pour chaque type de minerai. Voici un exemple de configuration :

```yaml
# Configuration des seuils par type de minerai
# Format:
#   suspicious: Seuil pour déclencher une alerte
#   very-suspicious: Seuil pour bannir le joueur
#   max-realistic: Quantité maximale réaliste pour un joueur normal

diamond_ore:
  suspicious: 20
  very-suspicious: 30
  max-realistic: 15

ancient_debris:
  suspicious: 6
  very-suspicious: 8
  max-realistic: 4

# ... autres minerais ...

# Paramètres généraux
settings:
  # Durée de la fenêtre de détection en secondes (20 minutes par défaut)
  detection-window: 1200
  
  # Messages personnalisables
  warning-message: "&cAttention: Vous avez miné %amount% %ore% en peu de temps. Soyez prudent !"
  ban-message: "&cVous avez été banni pour minage suspect de %ore% (Quantité: %amount% en 20 minutes)"
```

## 🎮 Commandes

- `/7s7core reload` - Recharge la configuration
- `/7s7core help` - Affiche l'aide
- `/orealert` - Alias de la commande principale

### Exemples :
```
/7s7core reload
/orealert help
```

## 🔒 Permissions

- `7s7core.admin` - Accès à toutes les commandes d'administration
  - Inclut automatiquement `7s7core.alerts` et `7s7core.bypass`
  - Par défaut : OP uniquement

- `7s7core.alerts` - Reçoit les alertes de minage suspect
  - Par défaut : OP uniquement

- `7s7core.bypass` - Contourne la détection de minage suspect
  - Par défaut : OP uniquement

## 📝 Notes

- Les alertes sont envoyées à tous les opérateurs en ligne
- Les logs sont enregistrés dans la console du serveur
- Compatible avec les versions 1.20.1 de Spigot/Paper
