import unittest
import os

from mayat.frontends import C, Python, TS_C, TS_Java, TS_Python

C_SAMPLE_DIR = "c_samples"
JAVA_SAMPLE_DIR = "java_samples"
PYTHON_SAMPLE_DIR = "python_samples"

class TestMayat(unittest.TestCase):
    def test_TS_C_frontend(self):
        c_files = [os.path.join(C_SAMPLE_DIR, f) for f in os.listdir(C_SAMPLE_DIR)]
        TS_C.main(c_files, "*", 5)
    
    def test_TS_Java_frontend(self):
        java_files = [os.path.join(JAVA_SAMPLE_DIR, f) for f in os.listdir(JAVA_SAMPLE_DIR)]
        TS_Java.main(java_files, "*", 20)
    
    def test_TS_Python_frontend(self):
        python_files = [os.path.join(PYTHON_SAMPLE_DIR, f) for f in os.listdir(PYTHON_SAMPLE_DIR)]
        TS_Java.main(python_files, "*", 5)

if __name__ == '__main__':
    unittest.main()