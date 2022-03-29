#!/usr/bin/env python

"""The setup script."""

from setuptools import setup, find_packages
from pkg_resources import parse_requirements as _parse_requirements

def parse_requirements(s):
    return [
        str(r)
        for r in
        _parse_requirements(s)
    ]


with open('requirements.txt') as requirements_file:
    requirements = parse_requirements(requirements_file.read())
with open('requirements_dev.txt') as requirements_dev_file:
    test_requirements = parse_requirements(requirements_dev_file.read())

setup(
    author="Maxwell Yang",
    python_requires='>=3.5',
    classifiers=[
        'Development Status :: 2 - Pre-Alpha',
        'Intended Audience :: Developers',
        'Natural Language :: English',
        'Programming Language :: Python :: 3.7',
        'Programming Language :: Python :: 3.8',
        'Programming Language :: Python :: 3.9',
        'Programming Language :: Python :: 3.10',
    ],
    description="Anubis Plagerism Detection",
    entry_points={
        'console_scripts': [
            'anubis=anubis.cli:main',
        ],
    },
    install_requires=requirements,
    include_package_data=True,
    keywords='anubis',
    name='anubis-pd',
    packages=find_packages(include=['anubis_pd', 'anubis_pd.*']),
    setup_requires=setup_requirements,
    test_suite='tests',
    tests_require=test_requirements,
    version='0.0.0',
    zip_safe=False,
)
