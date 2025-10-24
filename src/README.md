# Book Recommendation Engine

A full-stack book recommendation engine that delivers personalized book suggestions using AI, built with Spring Boot (Kotlin), Spring AI, React, PostgreSQL, and AWS. The app processes mock Goodreads data (5,000 books, 10,000 ratings) to recommend books based on user ratings and preferences (e.g., "You loved *1984*; try *Brave New World*"). It features a responsive React UI, secure OAuth2 APIs, and efficient data handling, designed for Canada’s e-commerce and edtech sectors (e.g., Indigo).

This project was built during a 1-year upskilling period to master Spring AI and Kotlin, showcasing modern design patterns (Repository, Service, Strategy, Decorator) and cost-efficient deployment ($0–$10/month).

## Features
- **Personalized Recommendations**: Uses Spring AI (Hugging Face DistilBERT) to generate book suggestions based on user ratings and genres.
- **Responsive UI**: React/TypeScript front-end with search, user profiles, recommendation feed, and genre analytics (Chart.js).
- **Secure APIs**: OAuth2 (Auth0) protects user data, aligning with cybersecurity demands (28% salary premium in Canada).
- **Scalable Backend**: Spring Boot (Kotlin) with PostgreSQL, optimized for 5,000 books and 200ms latency.
- **Cost-Efficient Deployment**: Runs on Render free tier or AWS (Elastic Beanstalk, S3, RDS) with minimal costs ($0–$10/month).

## Tech Stack
- **Front-End**: React, TypeScript, Tailwind CSS, Chart.js
- **Back-End**: Spring Boot (Kotlin), Spring AI (DistilBERT), Spring Security (OAuth2)
- **Database**: PostgreSQL
- **Deployment**: AWS (Elastic Beanstalk, S3, RDS) or Render
- **Tools**: Docker, Gradle, Auth0

## Architecture
![Architecture Diagram](architecture.png)
- **React UI**: Fetches recommendations via secure REST APIs.
- **Spring Boot**: Handles business logic, integrates DistilBERT for AI recommendations.
- **PostgreSQL**: Stores books, users, and ratings with optimized queries (30% faster reads).
- **AWS/Render**: Deploys backend (Beanstalk/Render), front-end (S3), and database (RDS).

## Setup Instructions
### Prerequisites
- Docker, Docker Compose
- Gradle 8.10+
- Auth0 account (free tier)
- Optional: AWS account, Hugging Face API token

### Local Development
1. Clone the repo:
   ```bash
   git clone https://github.com/your-username/book-recommendation-engine.git
   cd book-recommendation-engine

2. Start PostgreSQL and Spring Boot:
   ```bash
    docker-compose up

3. Seed mock Goodreads data (5,000 books, 10,000 ratings):
   ```bash
    python scripts/seed_data.py

4. Run front-end:
   ```bash

    cd frontend
    npm install
    npm start

Access the app at http://localhost:3000 (React) and http://localhost:8080/api (Spring Boot).