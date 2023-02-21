#!/usr/bin/env python

"""The setup script."""

from setuptools import setup, find_packages
from pkg_resources import parse_requirements as _parse_requirements


def parse_requirements(s):
    return [str(r) for r in _parse_requirements(s)]


with open("requirements.txt") as requirements_file:
    requirements = parse_requirements(requirements_file.read())
with open("requirements_dev.txt") as requirements_dev_file:
    test_requirements = parse_requirements(requirements_dev_file.read())

setup(
    name="Mayat",
    version="1.0.0",
    author="Tian(Maxwell) Yang",
    author_email="ty1146@nyu.edu",
    packages=find_packages(include=["mayat", "mayat.*"]),
    url="https://github.com/AnubisLMS/Mayat",
    license="LICENSE.txt",
    description="AST-based code similarity detection tool",
    long_description=open('README.md').read(),
    install_requires=requirements
)
