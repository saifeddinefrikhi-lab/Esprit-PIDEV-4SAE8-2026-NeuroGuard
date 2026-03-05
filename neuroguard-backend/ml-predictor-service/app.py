from flask import Flask, request, jsonify
from flask_cors import CORS
import joblib
import numpy as np
import pandas as pd
import os
import atexit
from feature_extraction import MedicalFeatureExtractor
from mock_model import MockRandomForestModel  # Import for unpickling

app = Flask(__name__)
CORS(app)

# Initialize feature extractor
feature_extractor = MedicalFeatureExtractor()

# Load pre-trained model and feature names (assumes files exist)
# In a real project you would train the model offline and save it.
model = None
feature_names = None

if os.path.exists('hospitalisation_model.pkl') and os.path.exists('feature_names.pkl'):
    model = joblib.load('hospitalisation_model.pkl')
    feature_names = joblib.load('feature_names.pkl')
    print("✓ Model loaded successfully")
    print(f"  Features: {len(feature_names)}")
else:
    print("⚠ WARNING: Model files not found. Please train the model first using train_model.py")
    print("  Run: python train_model.py")


def _to_bool(value):
    return str(value).strip().lower() in {"1", "true", "yes", "on"}


def register_eureka_if_enabled(port):
    if not _to_bool(os.getenv("EUREKA_ENABLED", "false")):
        print("ℹ Eureka disabled (set EUREKA_ENABLED=true to enable)")
        return

    try:
        import py_eureka_client.eureka_client as eureka_client
    except ImportError:
        print("⚠ py_eureka_client is not installed. Run: pip install py-eureka-client")
        return

    app_name = os.getenv("EUREKA_APP_NAME", "ml-predictor-service")
    host = os.getenv("EUREKA_INSTANCE_HOST", "localhost")
    server_url = os.getenv("EUREKA_SERVER_URL", "http://localhost:8761/eureka/")
    instance_id = os.getenv("EUREKA_INSTANCE_ID", f"{app_name}:{host}:{port}")
    health_url = os.getenv("EUREKA_HEALTHCHECK_URL", f"http://{host}:{port}/health")
    home_url = os.getenv("EUREKA_HOMEPAGE_URL", f"http://{host}:{port}/")
    status_url = os.getenv("EUREKA_STATUSPAGE_URL", health_url)
    instance_ip = os.getenv("EUREKA_INSTANCE_IP", "")

    init_kwargs = {
        "eureka_server": server_url,
        "app_name": app_name,
        "instance_port": port,
        "instance_host": host,
        "instance_id": instance_id,
        "health_check_url": health_url,
        "home_page_url": home_url,
        "status_page_url": status_url,
    }

    if instance_ip:
        init_kwargs["instance_ip"] = instance_ip

    try:
        eureka_client.init(**init_kwargs)
        print(f"✓ Registered to Eureka: {app_name} ({instance_id})")

        def _stop_eureka():
            try:
                eureka_client.stop()
                print("✓ Deregistered from Eureka")
            except Exception:
                pass

        atexit.register(_stop_eureka)
    except Exception as err:
        print(f"⚠ Eureka registration failed: {err}")


@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'model_loaded': model is not None,
        'features_count': len(feature_names) if feature_names else 0
    })


@app.route('/predict', methods=['POST'])
def predict():
    """
    Predict hospitalization risk from patient features
    
    Expected JSON payload (matching Java PatientFeatures):
    {
        "patientId": 123,
        "age": 65,
        "gender": "MALE",              // or 1 for MALE, 0 for FEMALE, 2 for OTHER
        "progressionStage": "MODERATE", // or 1=MILD, 2=MODERATE, 3=SEVERE
        "yearsSinceDiagnosis": 3,
        "comorbidityCount": 2,
        "allergyCount": 1,
        "hasGeneticRisk": true,        // or 1/0
        "hasFamilyHistory": true,      // or 1/0
        "surgeryCount": 1,
        "caregiverCount": 1,
        "providerCount": 2
    }
    
    OR legacy medical history format (will be converted):
    {
        "patientId": "123",
        "diagnosis": "Parkinson's disease",
        "diagnosis_date": "2023-05-15",
        "genetic_risk": "LRRK2 mutation positive",
        ...
    }
    """
    if model is None or feature_names is None:
        return jsonify({
            'error': 'Model not loaded. Please train the model first using train_model.py',
            'instructions': 'Run: python train_model.py'
        }), 503
    
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({'error': 'No JSON data provided'}), 400
        
        patient_id = data.get('patientId') or data.get('patient_id')
        
        # Check if this is PatientFeatures format or legacy format
        if 'age' in data and 'gender' in data:
            # Direct PatientFeatures format
            features = extract_patient_features(data, feature_names)
        else:
            # Legacy medical history format - extract features
            extracted = feature_extractor.extract_features(data)
            features = extract_patient_features(extracted, feature_names)
        
        # Convert to named DataFrame for sklearn to preserve feature alignment
        X = pd.DataFrame([features], columns=feature_names)
        
        # Make prediction
        proba = model.predict_proba(X)[0][1]   # probability of hospitalization
        pred = int(model.predict(X)[0])
        
        # Determine risk level
        risk_level = get_risk_level(proba)
        
        response = {
            'patientId': patient_id,
            'prediction': pred,
            'probability': float(round(proba, 4)),
            'riskLevel': risk_level,
            'riskPercentage': float(round(proba * 100, 2)),
            'recommendation': get_recommendation(risk_level, proba)
        }
        
        return jsonify(response)
        
    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({
            'error': f'Prediction failed: {str(e)}',
            'details': 'Check that all required fields are provided'
        }), 400


@app.route('/predict/batch', methods=['POST'])
def predict_batch():
    """
    Predict hospitalization risk for multiple patients
    
    Expected JSON payload:
    {
        "patients": [
            { PatientFeatures object 1 },
            { PatientFeatures object 2 },
            ...
        ]
    }
    """
    if model is None or feature_names is None:
        return jsonify({'error': 'Model not loaded'}), 503
    
    try:
        data = request.get_json()
        patients = data.get('patients', [])
        
        if not patients:
            return jsonify({'error': 'No patients data provided'}), 400
        
        results = []
        for patient_data in patients:
            patient_id = patient_data.get('patientId') or patient_data.get('patient_id')
            
            # Extract features
            if 'age' in patient_data and 'gender' in patient_data:
                features = extract_patient_features(patient_data, feature_names)
            else:
                extracted = feature_extractor.extract_features(patient_data)
                features = extract_patient_features(extracted, feature_names)
            
            # Predict
            X = pd.DataFrame([features], columns=feature_names)
            proba = model.predict_proba(X)[0][1]
            pred = int(model.predict(X)[0])
            risk_level = get_risk_level(proba)
            
            results.append({
                'patientId': patient_id,
                'prediction': pred,
                'probability': float(round(proba, 4)),
                'riskLevel': risk_level,
                'riskPercentage': float(round(proba * 100, 2))
            })
        
        return jsonify({
            'predictions': results,
            'totalPatients': len(results)
        })
        
    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({'error': f'Batch prediction failed: {str(e)}'}), 400


def extract_patient_features(data, feature_names):
    """
    Extract features in the correct order matching feature_names
    Handles String values for gender and progressionStage
    """
    # Gender encoding
    gender_map = {'MALE': 1, 'M': 1, 'FEMALE': 0, 'F': 0, 'OTHER': 2}
    
    # Progression stage encoding
    stage_map = {'MILD': 1, 'MODERATE': 2, 'SEVERE': 3, 'EARLY': 1, 'ADVANCED': 3}

    def to_int(value, default=0):
        try:
            if value is None or str(value).strip() == '':
                return default
            return int(float(value))
        except Exception:
            return default

    def to_float(value, default=0.0):
        try:
            if value is None or str(value).strip() == '':
                return default
            return float(value)
        except Exception:
            return default
    
    features = []
    for name in feature_names:
        value = data.get(name, 0)
        
        # Handle gender encoding
        if name == 'gender':
            if isinstance(value, str):
                value = gender_map.get(value.upper(), 1)
            features.append(to_int(value, default=1))
        
        # Handle progression stage encoding
        elif name == 'progressionStage':
            if isinstance(value, str):
                value = stage_map.get(value.upper(), 2)
            features.append(to_int(value, default=2))
        
        # Handle boolean values
        elif name in ['hasGeneticRisk', 'hasFamilyHistory']:
            if isinstance(value, bool):
                features.append(1 if value else 0)
            else:
                features.append(to_int(value, default=0))
        
        # All other numeric values
        else:
            if '.' in str(value):
                features.append(to_float(value, default=0.0))
            else:
                features.append(to_int(value, default=0))
    
    return features


def get_risk_level(probability):
    """Determine risk level from probability"""
    if probability >= 0.7:
        return 'CRITICAL'
    elif probability >= 0.5:
        return 'HIGH'
    elif probability >= 0.3:
        return 'MODERATE'
    elif probability >= 0.15:
        return 'LOW'
    else:
        return 'MINIMAL'


def get_recommendation(risk_level, probability):
    """Get recommendation based on risk level"""
    recommendations = {
        'CRITICAL': 'URGENT: Immediate medical evaluation recommended. High risk of hospitalization.',
        'HIGH': 'Schedule medical consultation soon. Increased monitoring recommended.',
        'MODERATE': 'Regular monitoring advised. Discuss with healthcare provider.',
        'LOW': 'Standard care and monitoring. Maintain current treatment plan.',
        'MINIMAL': 'Continue standard care. Low hospitalization risk.'
    }
    return recommendations.get(risk_level, 'Consult with healthcare provider.')

if __name__ == '__main__':
    service_host = os.getenv("SERVICE_HOST", "0.0.0.0")
    service_port = int(os.getenv("SERVICE_PORT", "5000"))
    debug = _to_bool(os.getenv("DEBUG", "true"))

    # Avoid duplicate registration when Flask reloader is active.
    is_main_process = os.environ.get("WERKZEUG_RUN_MAIN") == "true" or not debug
    if is_main_process:
        register_eureka_if_enabled(service_port)

    app.run(host=service_host, port=service_port, debug=debug)
