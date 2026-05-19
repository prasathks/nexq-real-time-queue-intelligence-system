from fastapi import FastAPI
from pydantic import BaseModel
from sklearn.linear_model import LinearRegression
import numpy as np
import pandas as pd

app = FastAPI(title="NexQ AI Wait Time Predictor")

# Mock historical data for training
# Features: [waiting_count, priority_weight (0=Normal, 1=Priority, 2=Emergency)]
# Target: actual wait time in minutes
historical_data = {
    'waiting_count': [0, 5, 10, 15, 2, 8, 12, 1, 6, 11, 20, 25, 5, 10, 15],
    'priority_weight': [0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 0, 0, 2, 1, 0],
    'wait_time': [2, 25, 50, 75, 5, 30, 50, 1, 15, 35, 100, 125, 10, 40, 75]
}

df = pd.DataFrame(historical_data)
X = df[['waiting_count', 'priority_weight']]
y = df['wait_time']

# Train a simple Linear Regression model
model = LinearRegression()
model.fit(X, y)

class PredictionRequest(BaseModel):
    waitingCount: int
    priorityWeight: int
    queueId: int # Included for future queue-specific logic

class PredictionResponse(BaseModel):
    estimatedWaitMinutes: int

@app.post("/predict", response_model=PredictionResponse)
def predict_wait_time(request: PredictionRequest):
    # Prepare features
    features = np.array([[request.waitingCount, request.priorityWeight]])
    
    # Predict
    predicted_time = model.predict(features)[0]
    
    # Floor to 0 if negative, round to nearest minute
    estimated_minutes = max(0, int(round(predicted_time)))
    
    # Emergency tokens bypass much of the wait, force a low cap if needed
    if request.priorityWeight == 2 and estimated_minutes > 15:
        estimated_minutes = 15
        
    return PredictionResponse(estimatedWaitMinutes=estimated_minutes)

@app.get("/health")
def health_check():
    return {"status": "ok"}
