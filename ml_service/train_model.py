import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.model_selection import train_test_split
import pickle
import os
import mysql.connector

print("=== IA PROFESSIONNELLE : DB + RANDOM FOREST REGRESSOR ===")

# 1. Configuration Base de données
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "skillpathdb"
}

def load_real_data():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        query = """
        SELECT 
            (SELECT COUNT(*) FROM certificate c WHERE c.student_id = u.id) as certifs,
            u.niveau,
            u.domaine,
            p.completed_modules,
            (SELECT COUNT(*) FROM module m WHERE m.course_id = p.course_id) as total_modules,
            co.category as course_category
        FROM user u
        JOIN user_course_progress p ON u.id = p.user_id
        JOIN course co ON p.course_id = co.id
        """
        df = pd.read_sql(query, conn)
        conn.close()
        if df.empty: return None
        
        print(f"-> {len(df)} données réelles extraites de la DB.")
        
        # Transformation
        df['progression'] = (df['completed_modules'] / df['total_modules'] * 100).fillna(0)
        def encode_level(l):
            l = str(l).lower()
            if 'avanc' in l or 'expert' in l: return 1.0
            if 'inter' in l or 'moyen' in l: return 0.5
            return 0.0
        df['niveau_val'] = df['niveau'].apply(encode_level)
        df['cat_match_val'] = df.apply(lambda r: 1 if str(r['domaine']).lower() in str(r['course_category']).lower() else 0, axis=1)
        
        return df[['certifs', 'niveau_val', 'progression', 'cat_match_val']].rename(
            columns={'niveau_val': 'niveau', 'cat_match_val': 'cat_match'}
        )
    except Exception as e:
        print(f"Note: Connexion DB impossible ({e}), utilisation de données simulées.")
        return None

# 2. Préparation des données
real_df = load_real_data()
n_synth = 15000
synth_df = pd.DataFrame({
    'certifs': np.random.randint(0, 11, n_synth),
    'niveau': np.random.choice([0.0, 0.5, 1.0], n_synth),
    'progression': np.random.randint(0, 101, n_synth),
    'cat_match': np.random.randint(0, 2, n_synth)
})

df = pd.concat([real_df, synth_df], ignore_index=True) if real_df is not None else synth_df

# 3. Calcul du pourcentage cible (Target)
def calculate_target(row):
    # Si la progression est à 100%, c'est 100% SUCCESS, point final.
    if row['progression'] >= 100:
        return 100.0
    
    # Sinon, la progression compte pour 90% du score final
    score = (row['progression'] * 0.9) + (row['niveau'] * 5) + (row['cat_match'] * 5)
    return max(0, min(99.0, score))

df['target_percentage'] = df.apply(calculate_target, axis=1)

# On ajoute 5000 exemples supplémentaires de "100% progression" pour "marquer" le cerveau de l'IA
extra_100 = pd.DataFrame({
    'certifs': np.random.randint(0, 11, 5000),
    'niveau': np.random.choice([0.0, 0.5, 1.0], 5000),
    'progression': [100.0] * 5000,
    'cat_match': np.random.randint(0, 2, 5000),
    'target_percentage': [100.0] * 5000
})
df = pd.concat([df, extra_100], ignore_index=True)

# 4. Entraînement
X = df[['certifs', 'niveau', 'progression', 'cat_match']]
y = df['target_percentage']

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

model = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
model.fit(X_train, y_train)

# 5. Rapport
y_pred = model.predict(X_test)
print("\n" + "="*40)
print(f"Erreur Moyenne (MAE) : {mean_absolute_error(y_test, y_pred):.2f}%")
print(f"Précision (R2) : {r2_score(y_test, y_pred)*100:.2f}%")
print("="*40 + "\n")

# 6. Sauvegarde
with open(os.path.join(os.path.dirname(__file__), 'model.pkl'), 'wb') as f:
    pickle.dump(model, f)

print("=== IA ENTRAÎNÉE SUR VOS DONNÉES ET PRÊTE ! ===")
