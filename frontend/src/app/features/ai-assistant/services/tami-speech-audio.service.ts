import { Injectable } from '@angular/core';

interface SpeechBlipOptions {
  intervalMs?: number;
}

@Injectable({ providedIn: 'root' })
export class TamiSpeechAudioService {
  private audioContext?: AudioContext;
  private timer?: ReturnType<typeof setInterval>;
  private readonly defaultIntervalMs = 72;
  private readonly volume = 0.12;

  start(options: SpeechBlipOptions = {}): void {
    this.stop();

    if (typeof window === 'undefined') {
      return;
    }

    const intervalMs = Math.max(48, options.intervalMs ?? this.defaultIntervalMs);
    const context = this.getAudioContext();

    if (!context) {
      return;
    }

    if (context.state === 'suspended') {
      void context.resume().catch(() => undefined);
    }

    this.playBlip();
    this.timer = setInterval(() => this.playBlip(), intervalMs);
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }

  private getAudioContext(): AudioContext | undefined {
    if (this.audioContext) {
      return this.audioContext;
    }

    const AudioContextConstructor = window.AudioContext ?? window.webkitAudioContext;

    if (!AudioContextConstructor) {
      return undefined;
    }

    this.audioContext = new AudioContextConstructor();
    return this.audioContext;
  }

  private playBlip(): void {
    const context = this.audioContext;

    if (!context || context.state === 'closed') {
      return;
    }

    const now = context.currentTime;
    const duration = 0.038;
    const baseFrequency = 780 + Math.random() * 260;
    const endFrequency = baseFrequency * (1.25 + Math.random() * 0.3);

    const primary = context.createOscillator();
    primary.type = 'triangle';
    primary.frequency.setValueAtTime(baseFrequency, now);
    primary.frequency.exponentialRampToValueAtTime(endFrequency, now + duration);

    const sparkle = context.createOscillator();
    sparkle.type = 'sine';
    sparkle.frequency.setValueAtTime(baseFrequency * 2.02, now);
    sparkle.frequency.exponentialRampToValueAtTime(endFrequency * 2.08, now + duration * 0.85);

    const gain = context.createGain();
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.exponentialRampToValueAtTime(this.volume, now + 0.004);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + duration);

    const filter = context.createBiquadFilter();
    filter.type = 'bandpass';
    filter.frequency.setValueAtTime(1300, now);
    filter.Q.setValueAtTime(5, now);

    primary.connect(filter);
    sparkle.connect(filter);
    filter.connect(gain);
    gain.connect(context.destination);

    primary.start(now);
    sparkle.start(now);
    primary.stop(now + duration + 0.01);
    sparkle.stop(now + duration + 0.01);
  }
}

declare global {
  interface Window {
    webkitAudioContext?: typeof AudioContext;
  }
}
