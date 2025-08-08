#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Integrated Test Runner for Data Profiler Platform
Supports modular testing and full lifecycle testing
"""

import json
import requests
import time
import argparse
import sys
import os
from typing import Dict, List, Optional, Any


class TestRunner:
    """Main test runner class for the Data Profiler Platform"""
    
    def __init__(self, config_file: str = "test_config.json"):
        """Initialize test runner with configuration"""
        self.config = self._load_config(config_file)
        self.base_url = self.config.get("api_base_url", "http://localhost:8080")
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
        
        # Test results tracking
        self.tests_run = 0
        self.tests_passed = 0
        self.tests_failed = 0
        
    def _load_config(self, config_file: str) -> Dict:
        """Load test configuration from JSON file"""
        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            print(f"[ERROR] Configuration file {config_file} not found")
            sys.exit(1)
        except json.JSONDecodeError as e:
            print(f"[ERROR] Invalid JSON in configuration file: {e}")
            sys.exit(1)
    
    def _log_info(self, message: str):
        """Log info message"""
        print(f"[INFO] {message}")
    
    def _log_pass(self, message: str):
        """Log pass message"""
        print(f"[PASS] {message}")
        self.tests_passed += 1
    
    def _log_fail(self, message: str):
        """Log fail message"""
        print(f"[FAIL] {message}")
        self.tests_failed += 1
    
    def _make_request(self, method: str, endpoint: str, **kwargs) -> requests.Response:
        """Make HTTP request with error handling"""
        url = f"{self.base_url}{endpoint}"
        try:
            response = self.session.request(method, url, **kwargs)
            return response
        except requests.exceptions.RequestException as e:
            self._log_fail(f"Request failed: {e}")
            raise
    
    def _assert_status_code(self, response: requests.Response, expected: int, step_name: str, method: str, url: str, **kwargs):
        """Assert response status code and log request details on failure"""
        self.tests_run += 1
        if response.status_code == expected:
            self._log_pass(f"{step_name} (Status: {response.status_code})")
            return True
        else:
            self._log_fail(f"{step_name} - Expected {expected}, got {response.status_code}")
            print(f"    Request URL: {url}")
            print(f"    Request Method: {method}")
            if 'json' in kwargs:
                print(f"    Request Body: {json.dumps(kwargs['json'], indent=4)}")
            elif 'data' in kwargs:
                print(f"    Request Data: {kwargs['data']}")
            if 'params' in kwargs:
                print(f"    Request Params: {kwargs['params']}")
            if response.text:
                print(f"    Response: {response.text}")
            return False
    
    def test_datasource_module(self) -> bool:
        """Test data source management module"""
        self._log_info("Running Data Source Module Test")
        print("=" * 50)
        
        datasource_id = None
        success = True
        
        try:
            # Step 1: Create data source
            test_datasource = self.config["test_data_sources"][0]["payload"]
            endpoint = "/datasources"
            method = "POST"
            response = self._make_request(method, endpoint, json=test_datasource)
            
            if self._assert_status_code(response, 201, "Step 1: Create Data Source", method, f"{self.base_url}{endpoint}", json=test_datasource):
                datasource_data = response.json()
                datasource_id = datasource_data.get("sourceId")
                if not datasource_id:
                    self._log_fail("Step 1: No sourceId in response")
                    return False
            else:
                return False
            
            # Step 2: Test connection
            endpoint = f"/datasources/{datasource_id}/test"
            method = "POST"
            response = self._make_request(method, endpoint)
            if not self._assert_status_code(response, 200, "Step 2: Test Connection", method, f"{self.base_url}{endpoint}"):
                success = False
            
            # Step 3: Get data source by ID
            endpoint = f"/datasources/{datasource_id}"
            method = "GET"
            response = self._make_request(method, endpoint)
            if not self._assert_status_code(response, 200, "Step 3: Get Data Source", method, f"{self.base_url}{endpoint}"):
                success = False
            
            # Step 4: Update data source
            updated_config = test_datasource.copy()
            updated_config["name"] = "Updated " + updated_config["name"]
            endpoint = f"/datasources/{datasource_id}"
            method = "PUT"
            response = self._make_request(method, endpoint, json=updated_config)
            if not self._assert_status_code(response, 200, "Step 4: Update Data Source", method, f"{self.base_url}{endpoint}", json=updated_config):
                success = False
            
            # Step 5: Verify update
            endpoint = f"/datasources/{datasource_id}"
            method = "GET"
            response = self._make_request(method, endpoint)
            if self._assert_status_code(response, 200, "Step 5: Verify Update", method, f"{self.base_url}{endpoint}"):
                updated_data = response.json()
                if updated_data.get("name") == updated_config["name"]:
                    self._log_pass("Step 5: Name update verified")
                else:
                    self._log_fail("Step 5: Name update not reflected")
                    success = False
            else:
                success = False
            
            # Step 6: Delete data source
            endpoint = f"/datasources/{datasource_id}"
            method = "DELETE"
            response = self._make_request(method, endpoint)
            if not self._assert_status_code(response, 204, "Step 6: Delete Data Source", method, f"{self.base_url}{endpoint}"):
                success = False
            
            # Step 7: Verify deletion
            endpoint = f"/datasources/{datasource_id}"
            method = "GET"
            response = self._make_request(method, endpoint)
            if not self._assert_status_code(response, 404, "Step 7: Verify Deletion", method, f"{self.base_url}{endpoint}"):
                success = False
                
        except Exception as e:
            self._log_fail(f"Data Source Module Test failed with exception: {e}")
            success = False
        
        return success
    
    def test_profiling_module(self) -> bool:
        """Test profiling task management module"""
        self._log_info("Running Profiling Module Test")
        print("=" * 50)
        
        datasource_id = None
        task_id = None
        success = True
        
        try:
            # Step 1: Create data source for profiling
            test_datasource = self.config["test_data_sources"][0]["payload"]
            endpoint = "/datasources"
            method = "POST"
            response = self._make_request(method, endpoint, json=test_datasource)
            
            if self._assert_status_code(response, 201, "Step 1: Create Data Source", method, f"{self.base_url}{endpoint}", json=test_datasource):
                datasource_data = response.json()
                datasource_id = datasource_data.get("sourceId")
                if not datasource_id:
                    self._log_fail("Step 1: No sourceId in response")
                    return False
            else:
                return False
            
            # Step 2: Start profiling task
            profiling_request = {
                "datasources": {
                    datasource_id: {}
                },
                "taskName": "Test Profiling Task"
            }
            endpoint = "/profiling/profiling-tasks"
            method = "POST"
            response = self._make_request(method, endpoint, json=profiling_request)
            
            if self._assert_status_code(response, 201, "Step 2: Start Profiling Task", method, f"{self.base_url}{endpoint}", json=profiling_request):
                task_data = response.json()
                task_id = task_data.get("taskId")
                if not task_id:
                    self._log_fail("Step 2: No taskId in response")
                    success = False
            else:
                success = False
            
            if task_id:
                # Step 3: Monitor task status
                max_attempts = 30
                attempt = 0
                task_completed = False
                
                while attempt < max_attempts and not task_completed:
                    endpoint = f"/profiling/task-status/{task_id}"
                    method = "GET"
                    response = self._make_request(method, endpoint)

                    
                    if response.status_code == 200:
                        status_data = response.json()
                        status = status_data.get("status", "UNKNOWN")
                        
                        if status in ["SUCCESS", "COMPLETED"]:
                            self._log_pass("Step 3: Task completed successfully")
                            task_completed = True
                        elif status in ["FAILED", "ERROR"]:
                            self._log_fail(f"Step 3: Task failed with status: {status}")
                            success = False
                            break
                        else:
                            self._log_info(f"Task status: {status}, waiting...")
                            time.sleep(2)
                    else:
                        self._log_fail(f"Step 3: Failed to get task status (Status: {response.status_code})")
                        success = False
                        break
                    
                    attempt += 1
                
                if not task_completed and attempt >= max_attempts:
                    self._log_fail("Step 3: Task did not complete within timeout")
                    success = False
                
                # Step 4: Delete profiling task
                endpoint = f"/profiling/profiling-tasks/{task_id}"
                method = "DELETE"
                response = self._make_request(method, endpoint)
                if not self._assert_status_code(response, 204, "Step 4: Delete Profiling Task", method, f"{self.base_url}{endpoint}"):
                    success = False
            
            # Cleanup: Delete data source
            if datasource_id:
                endpoint = f"/datasources/{datasource_id}"
                method = "DELETE"
                response = self._make_request(method, endpoint)
                self._assert_status_code(response, 204, "Cleanup: Delete Data Source", method, f"{self.base_url}{endpoint}")
                
        except Exception as e:
            self._log_fail(f"Profiling Module Test failed with exception: {e}")
            success = False
        
        return success
    
    def test_report_module(self) -> bool:
        """Test report query module"""
        self._log_info("Running Report Module Test")
        print("=" * 50)
        
        datasource_id = None
        task_id = None
        success = True
        
        try:
            # Step 1: Create data source
            test_datasource = self.config["test_data_sources"][0]["payload"]
            endpoint = "/datasources"
            method = "POST"
            response = self._make_request(method, endpoint, json=test_datasource)
            
            if self._assert_status_code(response, 201, "Step 1: Create Data Source", method, f"{self.base_url}{endpoint}", json=test_datasource):
                datasource_data = response.json()
                datasource_id = datasource_data.get("sourceId")
                if not datasource_id:
                    self._log_fail("Step 1: No sourceId in response")
                    return False
            else:
                return False
            
            # Step 2: Start profiling task
            profiling_request = {
                "datasources": {
                    datasource_id: {}
                },
                "taskName": "Test Report Task"
            }
            endpoint = "/profiling/profiling-tasks"
            method = "POST"
            response = self._make_request(method, endpoint, json=profiling_request)
            
            if self._assert_status_code(response, 201, "Step 2: Start Profiling Task", method, f"{self.base_url}{endpoint}", json=profiling_request):
                task_data = response.json()
                task_id = task_data.get("taskId")
                if not task_id:
                    self._log_fail("Step 2: No taskId in response")
                    success = False
            else:
                success = False
            
            if task_id:
                # Step 3: Wait for task completion
                max_attempts = 30
                attempt = 0
                task_completed = False
                
                while attempt < max_attempts and not task_completed:
                    endpoint = f"/profiling/task-status/{task_id}"
                    method = "GET"
                    response = self._make_request(method, endpoint)

                    
                    if response.status_code == 200:
                        status_data = response.json()
                        status = status_data.get("status", "UNKNOWN")
                        
                        if status in ["SUCCESS", "COMPLETED"]:
                            self._log_pass("Step 3: Task completed successfully")
                            task_completed = True
                        elif status in ["FAILED", "ERROR"]:
                            self._log_fail(f"Step 3: Task failed with status: {status}")
                            success = False
                            break
                        else:
                            time.sleep(2)
                    else:
                        success = False
                        break
                    
                    attempt += 1
                
                if task_completed:
                    # Step 4: Query summary report
                    endpoint = f"/api/reports/{task_id}/summary"
                    method = "GET"
                    response = self._make_request(method, endpoint)
                    if not self._assert_status_code(response, 200, "Step 4: Query Summary Report", method, f"{self.base_url}{endpoint}"):
                        success = False
                    
                    # Step 5: Query detailed report
                    detailed_request = {
                        "dataSources": {
                            datasource_id: {}
                        }
                    }
                    endpoint = f"/api/reports/{task_id}/detailed"
                    method = "POST"
                    response = self._make_request(method, endpoint, json=detailed_request)
                    if not self._assert_status_code(response, 200, "Step 5: Query Detailed Report", method, f"{self.base_url}{endpoint}", json=detailed_request):
                        success = False
                
                # Cleanup: Delete profiling task
                endpoint = f"/profiling/profiling-tasks/{task_id}"
                method = "DELETE"
                response = self._make_request(method, endpoint)
                self._assert_status_code(response, 204, "Cleanup: Delete Profiling Task", method, f"{self.base_url}{endpoint}")
            
            # Cleanup: Delete data source
            if datasource_id:
                endpoint = f"/datasources/{datasource_id}"
                method = "DELETE"
                response = self._make_request(method, endpoint)
                self._assert_status_code(response, 204, "Cleanup: Delete Data Source", method, f"{self.base_url}{endpoint}")
                
        except Exception as e:
            self._log_fail(f"Report Module Test failed with exception: {e}")
            success = False
        
        return success
    
    def test_file_upload_lifecycle(self) -> bool:
        """Test file upload and complete lifecycle"""
        self._log_info("Running File Upload Lifecycle Test")
        print("=" * 50)
        
        datasource_id = None
        task_id = None
        success = True
        
        try:
            # Step 1: Upload file and create data source
            sample_file_path = self.config.get("sample_file_path", "sample_data.csv")
            
            if not os.path.exists(sample_file_path):
                self._log_fail(f"Step 1: Sample file not found: {sample_file_path}")
                return False
            
            with open(sample_file_path, 'rb') as f:
                files = {'file': (os.path.basename(sample_file_path), f, 'text/csv')}
                # Remove Content-Type header for file upload
                headers = {k: v for k, v in self.session.headers.items() if k.lower() != 'content-type'}
                url = f"{self.base_url}/files/upload"
                method = "POST"
                response = requests.post(url, files=files, headers=headers)
            
            if self._assert_status_code(response, 201, "Step 1: Upload File and Create Data Source", method, url, files=files):
                datasource_data = response.json()
                datasource_id = datasource_data.get("sourceId")
                if not datasource_id:
                    self._log_fail("Step 1: No sourceId in response")
                    return False
            else:
                return False
            
            # Step 2: Start profiling task
            profiling_request = {
                "datasources": {
                    datasource_id: {}
                },
                "taskName": "File Upload Lifecycle Test"
            }
            endpoint = "/profiling/profiling-tasks"
            method = "POST"
            response = self._make_request(method, endpoint, json=profiling_request)
            
            if self._assert_status_code(response, 201, "Step 2: Start Profiling Task", method, f"{self.base_url}{endpoint}", json=profiling_request):
                task_data = response.json()
                task_id = task_data.get("taskId")
                if not task_id:
                    self._log_fail("Step 2: No taskId in response")
                    success = False
            else:
                success = False
            
            if task_id:
                # Step 3: Wait for task completion
                max_attempts = 30
                attempt = 0
                task_completed = False
                
                while attempt < max_attempts and not task_completed:
                    endpoint = f"/profiling/task-status/{task_id}"
                    method = "GET"
                    response = self._make_request(method, endpoint)

                    
                    if response.status_code == 200:
                        status_data = response.json()
                        status = status_data.get("status", "UNKNOWN")
                        
                        if status in ["SUCCESS", "COMPLETED"]:
                            self._log_pass("Step 3: Task completed successfully")
                            task_completed = True
                        elif status in ["FAILED", "ERROR"]:
                            self._log_fail(f"Step 3: Task failed with status: {status}")
                            success = False
                            break
                        else:
                            self._log_info(f"Task status: {status}, waiting...")
                            time.sleep(2)
                    else:
                        success = False
                        break
                    
                    attempt += 1
                
                if task_completed:
                    # Step 4: Query summary report
                    endpoint = f"/api/reports/{task_id}/summary"
                method = "GET"
                response = self._make_request(method, endpoint)
                if not self._assert_status_code(response, 200, "Step 4: Query Summary Report", method, f"{self.base_url}{endpoint}"):
                    success = False
                    
                    # Step 5: Query detailed report
                    detailed_request = {
                        "dataSources": [datasource_id]
                    }
                    endpoint = f"/api/reports/{task_id}/detailed"
                    method = "POST"
                    response = self._make_request(method, endpoint, json=detailed_request)
                    if not self._assert_status_code(response, 200, "Step 5: Query Detailed Report", method, f"{self.base_url}{endpoint}", json=detailed_request):
                        success = False
                
                # Cleanup: Delete profiling task
                endpoint = f"/profiling/profiling-tasks/{task_id}"
                method = "DELETE"
                response = self._make_request(method, endpoint)
                self._assert_status_code(response, 204, "Cleanup: Delete Profiling Task", method, f"{self.base_url}{endpoint}")
            
            # Step 6: Delete data source
            if datasource_id:
                endpoint = f"/datasources/{datasource_id}"
                method = "DELETE"
                response = self._make_request(method, endpoint)
                if not self._assert_status_code(response, 204, "Step 6: Delete Data Source", method, f"{self.base_url}{endpoint}"):
                    success = False
                
                # Step 7: Verify deletion
                endpoint = f"/datasources/{datasource_id}"
                method = "GET"
                response = self._make_request(method, endpoint)
                if not self._assert_status_code(response, 404, "Step 7: Verify Deletion", method, f"{self.base_url}{endpoint}"):
                    success = False
                
        except Exception as e:
            self._log_fail(f"File Upload Lifecycle Test failed with exception: {e}")
            success = False
        
        return success
    
    def run_all_tests(self) -> bool:
        """Run all test modules in sequence"""
        self._log_info("Running All Test Modules")
        print("=" * 60)
        
        all_success = True
        
        # Test each module
        modules = [
            ("Data Source Module", self.test_datasource_module),
            ("Profiling Module", self.test_profiling_module),
            ("Report Module", self.test_report_module),
            ("File Upload Lifecycle", self.test_file_upload_lifecycle)
        ]
        
        for module_name, test_func in modules:
            print(f"\n{'='*20} {module_name} {'='*20}")
            try:
                success = test_func()
                if not success:
                    all_success = False
                    self._log_fail(f"{module_name} test failed")
                else:
                    self._log_pass(f"{module_name} test passed")
            except Exception as e:
                self._log_fail(f"{module_name} test failed with exception: {e}")
                all_success = False
        
        return all_success
    
    def print_summary(self):
        """Print test execution summary"""
        print("\n" + "=" * 50)
        print("SUMMARY:")
        print(f"Tests Run: {self.tests_run}, Passed: {self.tests_passed}, Failed: {self.tests_failed}")
        
        if self.tests_failed == 0:
            print("[SUCCESS] All tests passed!")
        else:
            print(f"[FAILURE] {self.tests_failed} test(s) failed")


def main():
    """Main function to run tests based on command line arguments"""
    parser = argparse.ArgumentParser(description="Data Profiler Platform Integration Test Runner")
    parser.add_argument("--module", 
                       choices=["datasource", "profiling", "report", "file", "all"], 
                       default="all",
                       help="Test module to run (default: all)")
    parser.add_argument("--config", 
                       default="test_config.json",
                       help="Configuration file path (default: test_config.json)")
    
    args = parser.parse_args()
    
    # Initialize test runner
    runner = TestRunner(args.config)
    
    # Run specified tests
    success = True
    
    if args.module == "datasource":
        success = runner.test_datasource_module()
    elif args.module == "profiling":
        success = runner.test_profiling_module()
    elif args.module == "report":
        success = runner.test_report_module()
    elif args.module == "file":
        success = runner.test_file_upload_lifecycle()
    elif args.module == "all":
        success = runner.run_all_tests()
    
    # Print summary
    runner.print_summary()
    
    # Exit with appropriate code
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()