from flask import Flask, request, jsonify
import pickle
import numpy as np
import os
import pandas as pd

app = Flask(__name__)

model_path = os.path.join(os.path.dirname(__file__), 'model.pkl')

try:
    with open(model_path, 'rb') as f:
        model = pickle.load(f)
    print("--- Modèle IA chargé et prêt ---")
except Exception as e:
    print(f"ERREUR CHARGEMENT MODÈLE : {e}")
    model = None

@app.route('/api/predict', methods=['POST'])
def predict():
    if model is None:
        return jsonify({'error': 'Modèle non chargé'}), 500

    try:
        data = request.get_json()
        features = data.get('features', [])
        
        # On s'assure que ce sont des nombres
        f_list = [float(x) for x in features]
        
        # Ordre attendu : certifs, niveau, progression, cat_match
        # On crée un DataFrame pour être SÛR que l'ordre des colonnes est respecté par le modèle
        cols = ['certifs', 'niveau', 'progression', 'cat_match']
        X_input = pd.DataFrame([f_list], columns=cols)
        
        # LOG DE DEBUGGING POUR VOUS
        print("\n=== NOUVELLE PRÉDICTION ===")
        print(f"Données reçues : {f_list}")
        print(f"-> Certificats : {f_list[0]}")
        print(f"-> Niveau      : {f_list[1]}")
        print(f"-> Progression : {f_list[2]}%")
        print(f"-> Cat Match   : {'OUI (1)' if f_list[3] == 1 else 'NON (0)'}")
        
        # --- FORCE 100% SUCCESS ---
        # Si la progression est à 100, on ne demande même pas à l'IA, on renvoie 100.
        if float(f_list[2]) >= 100.0:
            prediction = 100.0
        else:
            # Avec le Regressor, on utilise .predict() directement pour avoir le % (ex: 45.6)
            prediction = model.predict(X_input)[0]
        
        prob_success = float(prediction) / 100.0
        
        print(f"RESULTAT -> Probabilité de succès : {prediction:.2f}%")
        print("===========================\n")

        return jsonify({
            'prediction': {
                'probabilities': [1 - prob_success, prob_success]
            }
        })

    except Exception as e:
        print(f"Erreur lors de la prédiction : {e}")
        return jsonify({'error': str(e)}), 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
