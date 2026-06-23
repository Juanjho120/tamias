import { DOCUMENT } from '@angular/common';
import { Injectable, NgZone, inject } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TamiBrandingService {
  private readonly document = inject(DOCUMENT);
  private readonly ngZone = inject(NgZone);

  private observer?: MutationObserver;
  private scheduled = false;
  private started = false;

  private readonly styles = `
    .tami-robot-shell {
      --tami-robot-size: 1.8rem;
      --tami-robot-primary: #0d6efd;
      --tami-robot-accent: #6f42c1;
      --tami-robot-face: #eef6ff;
      --tami-robot-line: rgba(13, 110, 253, 0.42);
      --tami-robot-eye: #0d6efd;
      --tami-robot-mouth: #6f42c1;

      position: relative;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: var(--tami-robot-size);
      height: var(--tami-robot-size);
      min-width: var(--tami-robot-size);
      vertical-align: middle;
    }

    .tami-robot-shell--sm { --tami-robot-size: 1.45rem; }
    .tami-robot-shell--md { --tami-robot-size: 2rem; }
    .tami-robot-shell--lg { --tami-robot-size: 2.5rem; }

    .tami-robot-head {
      position: relative;
      display: inline-block;
      width: calc(var(--tami-robot-size) * 0.86);
      height: calc(var(--tami-robot-size) * 0.72);
      border: 2px solid var(--tami-robot-line);
      border-radius: 42% 42% 36% 36%;
      background:
        radial-gradient(circle at 25% 20%, rgba(255, 255, 255, 0.95) 0 12%, transparent 13%),
        linear-gradient(135deg, var(--tami-robot-face), #ffffff 72%);
      box-shadow: 0 0.35rem 0.8rem rgba(13, 110, 253, 0.16);
    }

    .tami-robot-antenna {
      position: absolute;
      top: calc(var(--tami-robot-size) * -0.04);
      left: 50%;
      width: 2px;
      height: calc(var(--tami-robot-size) * 0.22);
      background: var(--tami-robot-line);
      transform: translateX(-50%);
    }

    .tami-robot-antenna::before {
      content: '';
      position: absolute;
      top: -0.22rem;
      left: 50%;
      width: 0.36rem;
      height: 0.36rem;
      border-radius: 999px;
      background: linear-gradient(135deg, var(--tami-robot-primary), var(--tami-robot-accent));
      transform: translateX(-50%);
      box-shadow: 0 0 0.45rem rgba(13, 110, 253, 0.35);
    }

    .tami-robot-eye {
      position: absolute;
      top: 34%;
      width: calc(var(--tami-robot-size) * 0.12);
      height: calc(var(--tami-robot-size) * 0.12);
      border-radius: 999px;
      background: var(--tami-robot-eye);
      box-shadow: 0 0 0.3rem rgba(13, 110, 253, 0.35);
    }

    .tami-robot-eye--left { left: 27%; }
    .tami-robot-eye--right { right: 27%; }

    .tami-robot-mouth {
      position: absolute;
      left: 50%;
      bottom: 22%;
      width: calc(var(--tami-robot-size) * 0.28);
      height: 2px;
      border-radius: 999px;
      background: var(--tami-robot-mouth);
      transform: translateX(-50%);
      transform-origin: center;
    }

    .tami-nav-link {
      gap: 0.55rem;
    }

    .tami-nav-link .tami-robot-shell {
      margin-left: -0.1rem;
      margin-right: -0.15rem;
    }

    .tami-nav-link:hover .tami-robot-head,
    .tami-nav-link:focus-visible .tami-robot-head {
      animation: tamiRobotHover 0.72s ease-in-out;
    }

    .tami-nav-link:hover .tami-robot-antenna::before,
    .tami-nav-link:focus-visible .tami-robot-antenna::before {
      animation: tamiRobotGlow 0.72s ease-in-out;
    }

    .tami-title-enhanced,
    .tami-session-title-enhanced {
      display: flex !important;
      align-items: center;
      gap: 0.6rem;
    }

    .tami-session-title-enhanced .tami-robot-shell {
      --tami-robot-size: 1.85rem;
    }

    body.tami-is-speaking .tami-robot-session-title .tami-robot-mouth {
      animation: tamiRobotTalk 0.22s ease-in-out infinite;
    }

    body.tami-is-speaking .tami-robot-session-title .tami-robot-eye {
      animation: tamiRobotBlink 1.35s ease-in-out infinite;
    }

    @keyframes tamiRobotHover {
      0%, 100% { transform: translateY(0) rotate(0deg); }
      30% { transform: translateY(-2px) rotate(-5deg); }
      65% { transform: translateY(1px) rotate(4deg); }
    }

    @keyframes tamiRobotGlow {
      0%, 100% { box-shadow: 0 0 0.45rem rgba(13, 110, 253, 0.35); }
      50% { box-shadow: 0 0 0.9rem rgba(111, 66, 193, 0.48); }
    }

    @keyframes tamiRobotTalk {
      0%, 100% { height: 2px; border-radius: 999px; }
      50% { height: calc(var(--tami-robot-size) * 0.1); border-radius: 45%; }
    }

    @keyframes tamiRobotBlink {
      0%, 92%, 100% { transform: scaleY(1); }
      95% { transform: scaleY(0.18); }
    }
  `;

  start(): void {
    if (this.started || typeof MutationObserver === 'undefined') {
      return;
    }

    this.started = true;
    this.injectStyles();

    this.ngZone.runOutsideAngular(() => {
      window.setTimeout(() => this.enhanceAll(), 0);
      this.observer = new MutationObserver(() => this.scheduleEnhance());
      this.observer.observe(this.document.body, { childList: true, subtree: true, characterData: true });
    });
  }

  private scheduleEnhance(): void {
    if (this.scheduled) {
      return;
    }

    this.scheduled = true;
    window.setTimeout(() => {
      this.scheduled = false;
      this.enhanceAll();
    }, 80);
  }

  private enhanceAll(): void {
    this.enhanceAiNavigationLinks();
    this.enhanceAiPageTitle();
    this.enhanceAiSessionTitle();
    this.updateSpeakingState();
  }

  private enhanceAiNavigationLinks(): void {
    const links = Array.from(this.document.querySelectorAll<HTMLAnchorElement>('a[href$="/ai-assistant"]'));

    links.forEach((link) => {
      link.classList.add('tami-nav-link');
      this.replaceDirectText(link, ['AI Assistant', 'Asistente IA'], 'TAMI');

      if (link.querySelector('.tami-robot-shell')) {
        return;
      }

      const robot = this.createRobotElement('tami-robot-shell--sm tami-robot-nav');
      const icon = link.querySelector('i.bi, .bi, svg');

      if (icon) {
        icon.after(robot);
      } else {
        link.prepend(robot);
      }
    });
  }

  private enhanceAiPageTitle(): void {
    if (!this.isAiAssistantRoute()) {
      return;
    }

    const title = Array.from(this.document.querySelectorAll<HTMLElement>('h1'))
      .find((heading) => this.normalizeText(heading.textContent).startsWith('tami'));

    if (!title || title.querySelector('.tami-robot-shell')) {
      return;
    }

    title.classList.add('tami-title-enhanced');
    title.prepend(this.createRobotElement('tami-robot-shell--lg tami-robot-page-title'));
  }

  private enhanceAiSessionTitle(): void {
    if (!this.isAiAssistantRoute()) {
      return;
    }

    const headings = Array.from(this.document.querySelectorAll<HTMLElement>('h2'));
    const sessionTitle = headings.find((heading) => {
      const text = this.normalizeText(heading.textContent);
      return Boolean(text)
        && text !== 'sessions'
        && text !== 'sesiones'
        && !heading.closest('.list-group');
    });

    if (!sessionTitle || sessionTitle.querySelector('.tami-robot-shell')) {
      return;
    }

    sessionTitle.classList.add('tami-session-title-enhanced');
    sessionTitle.prepend(this.createRobotElement('tami-robot-shell--md tami-robot-session-title'));
  }

  private updateSpeakingState(): void {
    const isTyping = this.isAiAssistantRoute()
      && Boolean(this.document.querySelector('.ai-typing-cursor'));

    this.document.body.classList.toggle('tami-is-speaking', isTyping);
  }

  private createRobotElement(extraClasses: string): HTMLElement {
    const robot = this.document.createElement('span');
    robot.className = `tami-robot-shell ${extraClasses}`;
    robot.setAttribute('aria-hidden', 'true');
    robot.innerHTML = `
      <span class="tami-robot-antenna"></span>
      <span class="tami-robot-head">
        <span class="tami-robot-eye tami-robot-eye--left"></span>
        <span class="tami-robot-eye tami-robot-eye--right"></span>
        <span class="tami-robot-mouth"></span>
      </span>
    `;

    return robot;
  }

  private replaceDirectText(element: HTMLElement, values: string[], replacement: string): void {
    element.childNodes.forEach((node) => {
      if (node.nodeType !== Node.TEXT_NODE) {
        return;
      }

      const text = node.textContent ?? '';
      const normalized = this.normalizeWhitespace(text);

      if (values.some((value) => normalized === value)) {
        node.textContent = text.replace(normalized, replacement);
      }
    });
  }

  private isAiAssistantRoute(): boolean {
    return this.document.location.pathname.includes('/ai-assistant');
  }

  private normalizeText(value: string | null): string {
    return this.normalizeWhitespace(value ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  }

  private normalizeWhitespace(value: string): string {
    return value.replace(/\s+/g, ' ').trim();
  }

  private injectStyles(): void {
    if (this.document.getElementById('tamias-tami-branding-styles')) {
      return;
    }

    const style = this.document.createElement('style');
    style.id = 'tamias-tami-branding-styles';
    style.textContent = this.styles;
    this.document.head.appendChild(style);
  }
}
