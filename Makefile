LINT_DIRECTORIES := \
	anubis_pd \
	tests

COMMIT := $(shell git rev-parse HEAD)
LINT_FILES := $(shell find $(LINT_DIRECTORIES) -name '*.py' | xargs)

help:
	@echo 'For convenience'
	@echo
	@echo 'Available make targets:'
	@grep PHONY: Makefile | cut -d: -f2 | sed '1d;s/^/make/'

.PHONY: venv                # Create virtualenv
venv:
	@if [ ! -d venv ]; then \
		python3 -m venv venv; \
		./venv/bin/pip install -r ./requirements.txt -r ./requirements_dev.txt; \
	fi

.PHONY: lint                # Run black on lint directories
lint: venv
	@echo 'black to stylize'
	./venv/bin/black $(LINT_FILES)

.PHONY: coverage            # Run tests and generate a coverage report
coverage:
	env COVERAGE=1 ./tests/test.sh
	./venv/bin/coverage report -m

.PHONY: clean               # Clean directories
clean:
	rm -rf $$(find -name __pycache__) venv .data
