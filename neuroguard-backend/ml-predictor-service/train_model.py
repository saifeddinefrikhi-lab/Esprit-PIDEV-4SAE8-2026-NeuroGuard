"""
Train hospitalization risk prediction model
Uses medical history features to predict hospitalization risk
"""
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix, roc_auc_score
import joblib
import os
from generate_training_data import generate_training_data
from feature_extraction import MedicalFeatureExtractor


def train_model():
    """Train the hospitalization risk prediction model"""
    
    # Step 1: Generate or load training data
    training_file = 'data/training_data.csv'
    print("Generating refreshed training data (synthetic + medical history profiles)...")
    df = generate_training_data(
        n_samples=500,
        output_file=training_file,
        medical_history_file='data/medical_history_db.csv'
    )
    
    print(f"Loaded {len(df)} training samples")
    print(f"Hospitalization rate: {df['hospitalized'].mean():.2%}")
    
    # Step 2: Prepare features and target
    # Drop patientId and target variable
    feature_columns = [col for col in df.columns if col not in ['patientId', 'patient_id', 'hospitalized']]
    X = df[feature_columns]
    y = df['hospitalized']
    
    print(f"\nFeatures ({len(feature_columns)}):")
    for i, feat in enumerate(feature_columns, 1):
        print(f"  {i}. {feat}")
    
    # Verify features match PatientFeatures class (excluding patientId)
    expected_features = ['age', 'gender', 'progressionStage', 'yearsSinceDiagnosis',
                        'comorbidityCount', 'allergyCount', 'hasGeneticRisk',
                        'hasFamilyHistory', 'surgeryCount', 'caregiverCount', 'providerCount']
    
    if set(feature_columns) == set(expected_features):
        print("\n✓ Features match Java PatientFeatures class")
    else:
        print(f"\n⚠ Feature mismatch!")
        print(f"  Expected: {expected_features}")
        print(f"  Got: {feature_columns}")
    
    # Step 3: Train/test split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )
    
    print(f"\nTraining set: {len(X_train)} samples")
    print(f"Test set: {len(X_test)} samples")
    
    # Step 4: Train model
    print("\nTraining Random Forest model...")
    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        min_samples_split=5,
        min_samples_leaf=2,
        random_state=42,
        class_weight='balanced'  # Handle class imbalance
    )
    model.fit(X_train, y_train)
    
    # Step 5: Evaluate model
    print("\n=== Model Evaluation ===")
    train_score = model.score(X_train, y_train)
    test_score = model.score(X_test, y_test)
    
    print(f"Training accuracy: {train_score:.3f}")
    print(f"Test accuracy: {test_score:.3f}")
    
    # Predictions
    y_pred = model.predict(X_test)
    y_pred_proba = model.predict_proba(X_test)[:, 1]
    
    # Classification report
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred, target_names=['Not Hospitalized', 'Hospitalized']))
    
    # Confusion matrix
    print("\nConfusion Matrix:")
    print(confusion_matrix(y_test, y_pred))
    
    # ROC AUC
    try:
        roc_auc = roc_auc_score(y_test, y_pred_proba)
        print(f"\nROC AUC Score: {roc_auc:.3f}")
    except:
        print("\nCould not calculate ROC AUC (need positive samples in test set)")
    
    # Feature importance
    print("\n=== Feature Importance ===")
    feature_importance = pd.DataFrame({
        'feature': feature_columns,
        'importance': model.feature_importances_
    }).sort_values('importance', ascending=False)
    
    print(feature_importance.to_string(index=False))
    
    # Step 6: Save model and feature names
    print("\nSaving model...")
    joblib.dump(model, 'hospitalisation_model.pkl')
    joblib.dump(feature_columns, 'feature_names.pkl')
    
    print("\n✓ Model trained and saved successfully!")
    print("Files created:")
    print("  - hospitalisation_model.pkl")
    print("  - feature_names.pkl")
    
    return model, feature_columns


if __name__ == '__main__':
    try:
        model, features = train_model()
    except Exception as e:
        print(f"\n❌ Error training model: {e}")
        print("\nMake sure scikit-learn is installed:")
        print("  pip install scikit-learn")
        import traceback
        traceback.print_exc()
