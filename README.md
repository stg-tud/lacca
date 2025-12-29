# LACCA

LACCA is a Kanban-style project management application developed as part of a Software Campus project. It provides an intuitive interface for managing tasks using a Kanban board while applying local-first principles to enable collaborative work without relying on centralized cloud services.

The project is part of the research initiative “Local-First Approaches for Cloud-Agnostic Collaborative Applications”, exploring how collaborative applications can remain responsive, resilient, and privacy-preserving by synchronizing data directly between peers and functioning fully offline.

## Features

- **Kanban board** with columns representing task states  
- Create, move, and edit task cards
- Modern web UI backed by a Scala-based backend
- Live development setup using **npm** and **sbt**
- Open to further contributions and extensions

## Tech Stack

- Scala.js & Laminar for the frontend
- CRDTs for decentralized state management
- Trystero for peer-to-peer synchronization
- Vite for development and bundling

Built following **local-first** and **cloud-agnostic** design principles.

## Installation

### Clone the repository

```bash
git clone https://github.com/stg-tud/lacca.git
cd lacca
```

### Install lacca with npm

```bash
  npm install
```

### Install dependencies and run the Scala backend

```bash
  sbt
  sbt:kanban> ~fastLinkJS
```

### Install and start the UI dev server

```bash
  npm run dev
```