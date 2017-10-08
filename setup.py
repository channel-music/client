from os import path

from setuptools import setup


here = path.abspath(path.dirname(__file__))


with open(path.join(here, 'README.rst')) as f:
    long_description = f.read()


def get_requirements(filename):
    with open(filename) as f:
        return f.read().splitlines()


setup(name='channel',
      version='0.1.0',
      # Author details
      author='Antonis Kalou',
      author_email='kalouantonis@protonmail.com',
      # Project details
      description='Self-hosted music streaming',
      long_description=long_description,
      url='https://github.com/kalouantonis/channel',
      license='Apache',

      classifiers=[
          # Project maturity
          'Development Status :: 3 - Alpha',

          # Intended audience
          'Intended Audience :: End Users/Desktop',
          'Topic :: Multimedia :: Sound/Audio :: Players',

          # Supported python versions
          'Programming Language :: Python',
          'Programming Language :: Python :: 3',
          'Programming Language :: Python :: 3.4',
          'Programming Language :: Python :: 3.5',
          'Programming Language :: Python :: 3.6',
          'Programming Language :: Python :: Implementation :: CPython',
          'Operating System :: OS Independent',
      ],
      install_requires=get_requirements('requirements.txt'),
      test_suite='channel.tests',
      tests_require=get_requirements('test-requirements.txt'))
