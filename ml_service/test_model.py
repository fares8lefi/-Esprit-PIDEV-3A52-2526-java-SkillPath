import pickle
import pandas as pd
import os

model_path = os.path.join(os.path.dirname(__file__), 'model.pkl')

if not os.path.exists(model_path):
    print("ERREUR : Le fichier model.pkl n'existe pas !")
else:
    with open(model_path, 'rb') as f:
        model = pickle.load(f)
    
    # Test avec un profil : 1 certif, niveau 0, progression 0, cat_match 1
    test_data = [[1.0, 0.0, 0.0, 1.0]]
    df = pd.DataFrame(test_data, columns=['certifs', 'niveau', 'progression', 'cat_match'])
    
    prob = model.predict_proba(df)[0][1]
    
    print("=== TEST MANUEL DU MODÈLE ===")
    print(f"Entrée : {test_data}")
    print(f"Probabilité de succès calculée : {prob * 100:.2f}%")
    print("=============================")
