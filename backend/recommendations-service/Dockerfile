FROM python:3.11-slim
WORKDIR /service
COPY requirements.lock .
RUN pip install --no-cache-dir -r requirements.lock
COPY recommender ./recommender
RUN useradd --uid 10001 --create-home service
USER 10001
EXPOSE 8085
CMD ["uvicorn", "recommender.api:app", "--host", "0.0.0.0", "--port", "8085"]
