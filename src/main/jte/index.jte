@import one.dastec.jcompare.model.DiffNode
@import one.dastec.jcompare.service.CompareService
@import java.util.List
@param String leftPath
@param String rightPath
@param String viewType
@param DiffNode diffResult
@param List<CompareService.DiffEntry> tableResult

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>JCompare - Directory Comparison</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        .status-added { color: #059669; }
        .status-removed { color: #dc2626; }
        .status-modified { color: #d97706; }
        .status-identical { color: #374151; }
        .status-moved, .status-moved_modified { color: #7c3aed; }
        .folder::before { content: "📁 "; }
        .file::before { content: "📄 "; }
        .file-java::before { content: "☕ "; }
        .file-xml::before { content: "📜 "; }
        .file-json::before { content: "{} "; }
        .file-yaml::before { content: "🔑 "; }
        .file-properties::before { content: "⚙️ "; }
        .spinner {
            border: 4px solid rgba(0, 0, 0, 0.1);
            width: 36px;
            height: 36px;
            border-radius: 50%;
            border-left-color: #09f;
            animation: spin 1s linear infinite;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body class="bg-gray-100 min-h-screen font-sans text-gray-900">
    <div id="loading-overlay" class="hidden fixed inset-0 bg-gray-600 bg-opacity-50 flex items-center justify-center z-[100]">
        <div class="bg-white p-6 rounded-lg shadow-xl flex flex-col items-center">
            <div class="spinner mb-4"></div>
            <p class="text-gray-700 font-semibold">Please wait...</p>
        </div>
    </div>

    <div class="max-w-full mx-auto p-4 md:p-6">
        <header class="mb-6 border-b border-gray-300 pb-4">
            <h1 class="text-3xl font-bold text-blue-800">JCompare</h1>
            <p class="text-gray-600">Compare Java applications (Maven/Ant) effortlessly.</p>
        </header>
        
        <main class="bg-white shadow-md rounded-lg p-8">
            <form method="get" action="/" class="space-y-6">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div class="space-y-2">
                        <label for="leftPath" class="block text-sm font-semibold text-gray-700">Left Directory</label>
                        <div class="flex">
                            <input type="text" id="leftPath" name="leftPath" value="${leftPath}" class="flex-grow p-2 border border-gray-300 rounded-l focus:outline-none bg-gray-50" readonly>
                            <button type="button" onclick="openDirectorySelector('leftPath')" class="bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded-r border-t border-r border-b border-gray-300 transition-colors">Select...</button>
                        </div>
                    </div>
                    <div class="space-y-2">
                        <label for="rightPath" class="block text-sm font-semibold text-gray-700">Right Directory</label>
                        <div class="flex">
                            <input type="text" id="rightPath" name="rightPath" value="${rightPath}" class="flex-grow p-2 border border-gray-300 rounded-l focus:outline-none bg-gray-50" readonly>
                            <button type="button" onclick="openDirectorySelector('rightPath')" class="bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded-r border-t border-r border-b border-gray-300 transition-colors">Select...</button>
                        </div>
                    </div>
                </div>
                
                <div class="flex flex-col sm:flex-row items-center justify-between border-t border-gray-200 pt-6 space-y-4 sm:space-y-0">
                    <div class="flex items-center space-x-6">
                        <span class="text-sm font-semibold text-gray-700">View Style:</span>
                        <div class="flex items-center space-x-4">
                            <label class="inline-flex items-center cursor-pointer">
                                <input type="radio" name="viewType" value="tree" checked="${"tree".equals(viewType)}" class="form-radio text-blue-600 h-4 w-4">
                                <span class="ml-2 text-sm text-gray-700">Tree View</span>
                            </label>
                            <label class="inline-flex items-center cursor-pointer">
                                <input type="radio" name="viewType" value="table" checked="${"table".equals(viewType)}" class="form-radio text-blue-600 h-4 w-4">
                                <span class="ml-2 text-sm text-gray-700">Table View</span>
                            </label>
                        </div>
                    </div>
                    <button type="submit" class="w-full sm:w-auto bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-8 rounded shadow-sm transition-all">Compare Now</button>
                </div>
            </form>

            @if(diffResult != null)
                <div class="mt-12 border-t border-gray-200 pt-8">
                    <div class="flex justify-between items-center mb-6">
                        <h2 class="text-2xl font-bold text-gray-800">Comparison Results</h2>
                        @if("table".equals(viewType) && tableResult != null)
                            <button onclick="exportToExcel()" class="bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-4 rounded shadow-sm transition-all flex items-center space-x-2">
                                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"></path></svg>
                                <span>Export to Excel</span>
                            </button>
                        @endif
                    </div>
                    
                    <div class="bg-white border border-gray-200 rounded overflow-hidden">
                        @if("table".equals(viewType) && tableResult != null)
                            <table class="min-w-full divide-y divide-gray-200">
                                <thead class="bg-gray-50">
                                    <tr>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Path</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            <div class="flex items-center space-x-2">
                                                <span>Type</span>
                                                <select id="typeFilter" onchange="applyFilters()" class="ml-1 bg-gray-50 border border-gray-300 text-gray-900 text-[10px] rounded focus:ring-blue-500 focus:border-blue-500 block p-0.5 outline-none font-normal lowercase">
                                                    <option value="all">All</option>
                                                    <option value="directory">Directory</option>
                                                    <option value="java">Java</option>
                                                    <option value="xml">XML</option>
                                                    <option value="json">JSON</option>
                                                    <option value="yaml">YAML</option>
                                                    <option value="props">Props</option>
                                                    <option value="file">File</option>
                                                </select>
                                            </div>
                                        </th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                            <div class="flex items-center space-x-2">
                                                <span>Status</span>
                                                <select id="statusFilter" onchange="applyFilters()" class="ml-1 bg-gray-50 border border-gray-300 text-gray-900 text-[10px] rounded focus:ring-blue-500 focus:border-blue-500 block p-0.5 outline-none font-normal lowercase">
                                                    <option value="all">All</option>
                                                    <option value="added">Added</option>
                                                    <option value="removed">Removed</option>
                                                    <option value="modified">Modified</option>
                                                    <option value="moved">Moved</option>
                                                    <option value="moved_modified">Moved Mod</option>
                                                    <option value="identical">Identical</option>
                                                </select>
                                            </div>
                                        </th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Diff %</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Added</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Mod</th>
                                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Del</th>
                                    </tr>
                                </thead>
                                <tbody class="bg-white divide-y divide-gray-200">
                                    @for(CompareService.DiffEntry entry : tableResult)
                                        <tr class="table-row ${entry.isDirectory() ? "" : "cursor-pointer hover:bg-blue-50"}" 
                                            data-type="${entry.isDirectory() ? "directory" : (entry.path().endsWith(".java") ? "java" : (entry.path().endsWith(".xml") ? "xml" : (entry.path().endsWith(".json") ? "json" : (entry.path().endsWith(".yaml") || entry.path().endsWith(".yml") ? "yaml" : (entry.path().endsWith(".properties") ? "props" : "file")))))}"
                                            data-status="${entry.status().name().toLowerCase()}"
                                            data-left="${leftPath}"
                                            data-right="${rightPath}"
                                            data-rel="${entry.relativePath()}"
                                            data-source="${entry.sourcePath()}"
                                            ondblclick="if(this.dataset.rel) openFileDiff(this.dataset.left, this.dataset.right, this.dataset.rel, this.dataset.source)">
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">${entry.path()}</td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                                @if(entry.isDirectory())
                                                    <span class="folder"></span> Directory
                                                @else
                                                    @if(entry.path().endsWith(".java"))
                                                        <span class="file-java"></span> Java
                                                    @else
                                                        @if(entry.path().endsWith(".xml"))
                                                            <span class="file-xml"></span> XML
                                                        @else
                                                            @if(entry.path().endsWith(".json"))
                                                                <span class="file-json"></span> JSON
                                                            @else
                                                                @if(entry.path().endsWith(".yaml") || entry.path().endsWith(".yml"))
                                                                    <span class="file-yaml"></span> YAML
                                                                @else
                                                                    @if(entry.path().endsWith(".properties"))
                                                                        <span class="file-properties"></span> Props
                                                                    @else
                                                                        <span class="file"></span> File
                                                                    @endif
                                                                @endif
                                                            @endif
                                                        @endif
                                                    @endif
                                                @endif
                                            </td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium status-${entry.status().name().toLowerCase()}">
                                                ${entry.status().name().replace("_", "-")}
                                                @if((entry.status() == DiffNode.DiffStatus.MOVED || entry.status() == DiffNode.DiffStatus.MOVED_MODIFIED) && entry.sourcePath() != null)
                                                    <div class="text-[10px] text-gray-400 font-normal">from ${entry.sourcePath()}</div>
                                                @endif
                                            </td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">${entry.isDirectory() ? "-" : String.valueOf(String.format("%.1f%%", entry.percentage()))}</td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-green-600 font-semibold">${entry.isDirectory() || entry.status() == DiffNode.DiffStatus.MOVED ? "-" : String.valueOf(entry.added())}</td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-amber-600 font-semibold">${entry.isDirectory() || entry.status() == DiffNode.DiffStatus.MOVED ? "-" : String.valueOf(entry.modified())}</td>
                                            <td class="px-6 py-4 whitespace-nowrap text-sm text-red-600 font-semibold">${entry.isDirectory() || entry.status() == DiffNode.DiffStatus.MOVED ? "-" : String.valueOf(entry.removed())}</td>
                                        </tr>
                                    @endfor
                                </tbody>
                            </table>
                        @else
                            <div class="p-6 overflow-x-auto">
                                <ul class="space-y-1">
                                    @template.diffNode(node = diffResult, leftPath = leftPath, rightPath = rightPath)
                                </ul>
                            </div>
                        @endif
                    </div>
                </div>
            @endif
        </main>
        
        <footer class="mt-8 text-center text-gray-500 text-xs">
            &copy; 2026 JCompare - Built with Spring Boot 4.0.2 & JTE
        </footer>
    </div>

    @template.directorySelector()
    @template.diffModal()
    <script>
        function showLoading() {
            document.getElementById('loading-overlay').classList.remove('hidden');
        }

        function hideLoading() {
            document.getElementById('loading-overlay').classList.add('hidden');
        }

        function openFileDiff(leftPath, rightPath, relativePath, sourcePath) {
            console.log("openFileDiff called", {leftPath, rightPath, relativePath, sourcePath});
            const modal = document.getElementById('diff-modal');
            const iframe = document.getElementById('diff-iframe');
            if (!modal || !iframe) {
                console.error("Modal or Iframe not found");
                return;
            }
            let url = "/diff?leftPath=" + encodeURIComponent(leftPath) + "&rightPath=" + encodeURIComponent(rightPath) + "&relativePath=" + encodeURIComponent(relativePath);
            if (sourcePath) {
                url += "&sourcePath=" + encodeURIComponent(sourcePath);
            }
            console.log("Loading URL:", url);
            showLoading();
            // Ensure previous handlers are cleared
            iframe.onload = function() {
                hideLoading();
            };
            iframe.onerror = function() {
                hideLoading();
            };
            iframe.src = url;
            modal.classList.remove('hidden');
        }

        function closeDiffModal() {
            const modal = document.getElementById('diff-modal');
            const iframe = document.getElementById('diff-iframe');
            iframe.src = 'about:blank';
            modal.classList.add('hidden');
        }

        function exportToExcel() {
            const leftPath = document.getElementById('leftPath').value;
            const rightPath = document.getElementById('rightPath').value;
            const typeFilter = document.getElementById('typeFilter').value;
            const statusFilter = document.getElementById('statusFilter').value;
            
            const url = "/export?leftPath=" + encodeURIComponent(leftPath) + 
                        "&rightPath=" + encodeURIComponent(rightPath) +
                        "&typeFilter=" + encodeURIComponent(typeFilter) +
                        "&statusFilter=" + encodeURIComponent(statusFilter);
            
            window.location.href = url;
        }

        // Local Storage persistence
        document.addEventListener('DOMContentLoaded', () => {
            const leftInput = document.getElementById('leftPath');
            const rightInput = document.getElementById('rightPath');

            // Load from localStorage if inputs are empty
            if (!leftInput.value) {
                const savedLeft = localStorage.getItem('jcompare_leftPath');
                if (savedLeft) leftInput.value = savedLeft;
            }
            if (!rightInput.value) {
                const savedRight = localStorage.getItem('jcompare_rightPath');
                if (savedRight) rightInput.value = savedRight;
            }

            // Save to localStorage when form is submitted
            document.querySelector('form').addEventListener('submit', () => {
                showLoading();
                localStorage.setItem('jcompare_leftPath', leftInput.value);
                localStorage.setItem('jcompare_rightPath', rightInput.value);
            });
        });

        // This function will be called by directorySelector.jte when a directory is selected
        function onDirectorySelected(inputId, path) {
            localStorage.setItem('jcompare_' + inputId, path);
        }

        function applyFilters() {
            const typeValue = document.getElementById('typeFilter').value;
            const statusValue = document.getElementById('statusFilter').value;
            const rows = document.querySelectorAll('.table-row');
            
            rows.forEach(row => {
                const typeMatch = typeValue === 'all' || row.dataset.type === typeValue;
                const statusMatch = statusValue === 'all' || row.dataset.status === statusValue;
                
                if (typeMatch && statusMatch) {
                    row.classList.remove('hidden');
                } else {
                    row.classList.add('hidden');
                }
            });
        }
    </script>
</body>
</html>
