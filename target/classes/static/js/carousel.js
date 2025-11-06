/**
 * Carousel Module
 * Handles automatic slide rotation and user navigation for the Champions Dashboard
 */
class CarouselManager {
    /**
     * Create a new CarouselManager instance
     * @param {Object} config - Configuration options
     * @param {number} config.autoSlideDelay - Delay between slides in ms (default: 5000)
     * @param {string} config.slideSelector - CSS selector for slides (default: '.carousel-slide')
     * @param {string} config.dotSelector - CSS selector for navigation dots (default: '.dot')
     */
    constructor(config = {}) {
        this.slideIndex = 0;
        this.autoSlideInterval = null;
        this.autoSlideDelay = config.autoSlideDelay || 5000;
        this.slideSelector = config.slideSelector || '.carousel-slide';
        this.dotSelector = config.dotSelector || '.dot';

        this.init();
    }

    /**
     * Initialize carousel on DOM load
     */
    init() {
        document.addEventListener('DOMContentLoaded', () => {
            const slides = this.getSlides();
            if (slides.length > 0) {
                this.showSlide(this.slideIndex);
                this.startAutoSlide();
            }
        });
    }

    /**
     * Get all carousel slides
     * @returns {HTMLCollection} Collection of slide elements
     */
    getSlides() {
        return document.getElementsByClassName(
            this.slideSelector.replace('.', '')
        );
    }

    /**
     * Get all carousel navigation dots
     * @returns {HTMLCollection} Collection of dot elements
     */
    getDots() {
        return document.getElementsByClassName(
            this.dotSelector.replace('.', '')
        );
    }

    /**
     * Display slide at the given index
     * @param {number} index - Slide index to display
     */
    showSlide(index) {
        const slides = this.getSlides();
        const dots = this.getDots();

        if (slides.length === 0) {
            return;
        }

        // Normalize index to stay within bounds
        this.slideIndex = this.normalizeIndex(index, slides.length);

        // Remove active class from all slides and dots
        this.removeActiveClass(slides);
        this.removeActiveClass(dots);

        // Add active class to current slide and corresponding dot
        slides[this.slideIndex].classList.add('active');

        if (dots.length > this.slideIndex) {
            dots[this.slideIndex].classList.add('active');
        }
    }

    /**
     * Normalize index to stay within array bounds
     * @param {number} index - Index to normalize
     * @param {number} length - Array length
     * @returns {number} Normalized index
     */
    normalizeIndex(index, length) {
        if (index >= length) {
            return 0;
        }
        if (index < 0) {
            return length - 1;
        }
        return index;
    }

    /**
     * Remove 'active' class from all elements in a collection
     * @param {HTMLCollection} elements - Elements to remove class from
     */
    removeActiveClass(elements) {
        Array.from(elements).forEach(element => {
            element.classList.remove('active');
        });
    }

    /**
     * Navigate to a specific slide
     * Triggered by dot click or external navigation
     * @param {number} index - Slide index to navigate to
     */
    goToSlide(index) {
        this.showSlide(index);
        this.resetAutoSlide();
    }

    /**
     * Navigate to the next slide
     */
    nextSlide() {
        this.showSlide(this.slideIndex + 1);
    }

    /**
     * Navigate to the previous slide
     */
    previousSlide() {
        this.showSlide(this.slideIndex - 1);
    }

    /**
     * Start automatic slide rotation
     */
    startAutoSlide() {
        this.autoSlideInterval = setInterval(() => {
            this.nextSlide();
        }, this.autoSlideDelay);
    }

    /**
     * Stop automatic slide rotation
     */
    stopAutoSlide() {
        if (this.autoSlideInterval) {
            clearInterval(this.autoSlideInterval);
            this.autoSlideInterval = null;
        }
    }

    /**
     * Reset automatic slide rotation
     * Used when user manually navigates to restart the timer
     */
    resetAutoSlide() {
        this.stopAutoSlide();
        this.startAutoSlide();
    }
}

// Initialize carousel with default configuration
const carousel = new CarouselManager({
    autoSlideDelay: 5000
});

/**
 * Global function for dot navigation
 * Exposed for onclick handlers in HTML
 * @param {number} index - Slide index to navigate to
 */
function currentSlide(index) {
    carousel.goToSlide(index);
}