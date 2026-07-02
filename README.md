# 🏃 Veloce — Fitness Tracking & Activity Sharing

Application mobile de suivi d'activités sportives (course, vélo, marche) avec tracking GPS en temps réel, calcul automatique des calories brûlées et fil social communautaire.

> Développée avec Flutter & Firebase

---

## 📱 Aperçu

Veloce permet aux utilisateurs d'enregistrer leurs séances sportives via GPS, de suivre leur progression dans le temps, et de partager leurs performances avec une communauté d'amis/abonnés — le tout avec un calcul de calories précis basé sur l'intensité réelle de l'effort (pas une simple estimation forfaitaire).

**Statut du projet** : 🚧 En développement

---

## ✨ Fonctionnalités

### MVP (v1)
- 🔐 Authentification (email, Google, Apple)
- 📍 Tracking GPS en temps réel (arrière-plan inclus)
- 🔥 Calcul dynamique des calories (formule MET ajustée à la vitesse et au dénivelé)
- 📊 Historique et statistiques d'activités (distance, allure, dénivelé, splits)
- 👥 Fil social (kudos, commentaires, abonnements)
- 🌙 Mode sombre natif
- 📴 Mode offline-first avec synchronisation différée

### Premium (à venir)
- 🏆 Segments personnalisés et classements
- ❤️ Zones de fréquence cardiaque (capteurs BLE)
- 📈 Analyse de performance avancée
- 🔗 Intégration Apple Health / Google Fit

---

## 🛠️ Stack technique

| Composant | Technologie |
|---|---|
| Framework mobile | Flutter (Dart, null-safety) |
| State management | Riverpod |
| Backend | Firebase (Auth, Firestore, Cloud Functions, Storage, FCM) |
| Cartographie | flutter_map (OpenStreetMap) |
| Tracking GPS | flutter_background_geolocation / geolocator + flutter_foreground_task |
| Stockage local | Hive / Drift |
| Paiements in-app | RevenueCat |
| Graphiques | fl_chart |
| Analytics & crash reporting | Firebase Analytics, Crashlytics |

---

## 📂 Structure du projet

```
lib/
├── core/                   # Constantes, thèmes, utils, extensions
├── features/
│   ├── auth/               # Authentification & onboarding
│   ├── tracking/           # Enregistrement GPS + moteur de calcul calories
│   ├── history/            # Historique et détail des activités
│   ├── feed/                # Fil social, kudos, commentaires
│   └── profile/            # Profil utilisateur, paramètres
├── shared/                 # Widgets réutilisables, modèles communs
└── main.dart

functions/                  # Cloud Functions (validation calories, agrégation stats)
```

---

## 🚀 Installation & lancement

### Prérequis
- Flutter SDK (dernière version stable)
- Un projet Firebase configuré (Auth, Firestore, Storage, Functions activés)
- Xcode (build iOS) / Android Studio (build Android)

### Étapes

```bash
# Cloner le projet
git clone <repo-url>
cd veloce

# Installer les dépendances
flutter pub get

# Générer les fichiers Riverpod / freezed
flutter pub run build_runner build --delete-conflicting-outputs

# Configurer Firebase
flutterfire configure

# Lancer l'app
flutter run
```

### Variables d'environnement / configuration Firebase
Placer les fichiers suivants (non versionnés, voir `.gitignore`) :
- `android/app/google-services.json`
- `ios/Runner/GoogleService-Info.plist`

---

## 🔑 Permissions requises

| Permission | Usage | Plateforme |
|---|---|---|
| Localisation (toujours) | Tracking GPS en arrière-plan pendant une activité | iOS + Android |
| Notifications | Kudos, nouveaux abonnés, rappels d'activité | iOS + Android |
| Photos | Ajout de photo de profil / photo d'activité | iOS + Android |

⚠️ La demande de permission "localisation toujours" nécessite une justification textuelle claire côté App Store et Play Store — voir `docs/store-compliance.md`.

---

## 🧮 Moteur de calcul des calories

Le calcul repose sur la formule MET (Metabolic Equivalent of Task) :

```
Calories = MET × poids(kg) × durée(heures)
```

- Le MET est ajusté dynamiquement selon la vitesse moyenne réelle détectée pendant l'activité
- Un facteur d'ajustement est appliqué selon le dénivelé positif cumulé
- Le calcul final est **revalidé côté serveur** (Cloud Function) pour éviter toute manipulation côté client

Détails et table des valeurs MET : `lib/features/tracking/calorie_engine.dart`

---

## 🔒 Sécurité

- Règles Firestore strictes : un utilisateur ne peut modifier que ses propres données
- Validation serveur des données sensibles (calories, distance) via Cloud Functions
- Aucune donnée de localisation brute stockée au-delà de ce qui est nécessaire à l'affichage du tracé

---

## 🗺️ Roadmap

- [ ] MVP : auth, tracking, calcul calories, historique, feed basique
- [ ] Intégration RevenueCat + feature flags premium
- [ ] Segments & classements
- [ ] Intégration Apple Health / Google Fit
- [ ] Version tablette / mode entraîneur

---
