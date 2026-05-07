import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface Weather {
  temperature: number;
  minTemperature: number;
  rainProbability: number;
  condition: string;
}

@Component({
  selector: 'app-weather-widget',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './weather-widget.component.html'
})
export class WeatherWidgetComponent implements OnInit {
  @Input() dayOfWeek: string = '';
  @Input() date: string = '';
  
  weather: Weather | null = null;
  loading = false;
  voiceActive = false;
  speechSynth: SpeechSynthesis | null = null;

  constructor(private http: HttpClient) {
    this.speechSynth = window.speechSynthesis;
  }

  ngOnInit(): void {
    this.loadWeather();
  }

  loadWeather(): void {
    this.loading = true;
    this.http.get<Weather>(`http://localhost:8080/api/schedules/weather/${this.dayOfWeek}/${this.date}`)
      .subscribe({
        next: (data) => {
          this.weather = data;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          this.weather = {
            temperature: 22,
            minTemperature: 18,
            rainProbability: 10,
            condition: '☀️ Clear'
          };
        }
      });
  }

  speakWeather(): void {
    if (!this.weather || !this.speechSynth) return;
    
    const message = `Weather for ${this.dayOfWeek}. Temperature ${Math.round(this.weather.temperature)} degrees. ${this.weather.condition}. Rain chance ${this.weather.rainProbability} percent.`;
    
    const utterance = new SpeechSynthesisUtterance(message);
    utterance.lang = 'en-US';
    utterance.rate = 0.9;
    this.speechSynth.speak(utterance);
    this.voiceActive = true;
    utterance.onend = () => { this.voiceActive = false; };
  }

  stopSpeaking(): void {
    if (this.speechSynth) {
      this.speechSynth.cancel();
      this.voiceActive = false;
    }
  }

  getWeatherIcon(): string {
    if (!this.weather) return '🌡️';
    if (this.weather.condition.includes('Clear')) return '☀️';
    if (this.weather.condition.includes('Cloudy')) return '⛅';
    if (this.weather.condition.includes('Rain')) return '🌧️';
    if (this.weather.condition.includes('Thunder')) return '⛈️';
    return '🌡️';
  }

  getRainAdvice(): string {
    if (!this.weather) return '';
    if (this.weather.rainProbability > 70) return '🌂 Bring umbrella!';
    if (this.weather.rainProbability > 40) return '☔ Maybe bring umbrella';
    return '🎒 No rain gear needed';
  }
}