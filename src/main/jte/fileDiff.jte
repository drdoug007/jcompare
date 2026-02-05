@import one.dastec.jcompare.service.CompareService
@import java.util.List
@param String leftPath
@param String rightPath
@param String relativePath
@param String fileName
@param CompareService.FileDiff fileDiff

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>JCompare - File Difference</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css">
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/plugins/autoloader/prism-autoloader.min.js"></script>
    <style>
        .line-added { background-color: #d1fae5; }
        .line-removed { background-color: #fee2e2; }
        .line-modified { background-color: #fef3c7; }
        .line-number { color: #9ca3af; text-align: right; width: 3rem; padding-right: 1rem; user-select: none; }
        /* Tighten spacing to avoid an extra blank line between rows */
        pre { margin: 0 !important; padding: 0 !important; line-height: 1.1 !important; border-radius: 0 !important; background: transparent !important; }
        code { display: block; white-space: pre; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace !important; font-size: 0.875rem !important; line-height: 1.1 !important; padding: 0 !important; }
        td { padding-top: 0 !important; padding-bottom: 0 !important; }
        tr { height: auto !important; }
    </style>
</head>
<body class="bg-gray-100 min-h-screen font-sans text-gray-900">
    <div class="max-w-full mx-auto p-2">
        <header class="mb-4 border-b border-gray-300 pb-2 flex justify-between items-center">
            <div>
                <h1 class="text-2xl font-bold text-blue-800">File Diff: ${fileName}</h1>
                <p class="text-sm text-gray-600">${relativePath}</p>
            </div>
            <div class="flex space-x-6 text-sm">
                <div class="flex flex-col items-center">
                    <span class="text-gray-500 font-semibold uppercase text-xs">Added</span>
                    <span class="text-green-600 font-bold">${fileDiff.added()}</span>
                </div>
                <div class="flex flex-col items-center">
                    <span class="text-gray-500 font-semibold uppercase text-xs">Removed</span>
                    <span class="text-red-600 font-bold">${fileDiff.removed()}</span>
                </div>
                <div class="flex flex-col items-center">
                    <span class="text-gray-500 font-semibold uppercase text-xs">Modified</span>
                    <span class="text-amber-600 font-bold">${fileDiff.modified()}</span>
                </div>
                <div class="flex flex-col items-center">
                    <span class="text-gray-500 font-semibold uppercase text-xs">Difference</span>
                    <span class="text-blue-600 font-bold">${String.format("%.1f%%", fileDiff.percentage())}</span>
                </div>
            </div>
            <button onclick="parent.closeDiffModal()" class="bg-gray-200 hover:bg-gray-300 text-gray-800 px-4 py-2 rounded transition-colors">Close</button>
        </header>

        <main class="bg-white shadow-md rounded-lg overflow-hidden border border-gray-200">
            <div class="grid grid-cols-2 bg-gray-50 border-b border-gray-200 font-semibold text-sm">
                <div class="p-2 border-r border-gray-200">Left: ${leftPath}</div>
                <div class="p-2">Right: ${rightPath}</div>
            </div>
            <div class="flex overflow-x-auto">
                <div class="w-1/2 border-r border-gray-200">
                    <table class="w-full border-collapse">
                        @for(int i = 0; i < fileDiff.lines().size(); i++)
                            @if(fileDiff.lines().get(i).left() != null)
                                <tr class="line-${fileDiff.lines().get(i).status().name().toLowerCase()}">
                                    <td class="line-number text-xs border-r border-gray-100">${i + 1}</td>
                                    <td class="px-2 py-0"><pre><code class="language-java">${fileDiff.lines().get(i).left()}</code></pre></td>
                                </tr>
                            @else
                                <tr class="bg-gray-50">
                                    <td class="line-number text-xs border-r border-gray-100"></td>
                                    <td class="px-2 py-0"></td>
                                </tr>
                            @endif
                        @endfor
                    </table>
                </div>
                <div class="w-1/2">
                    <table class="w-full border-collapse">
                        @for(int i = 0; i < fileDiff.lines().size(); i++)
                            @if(fileDiff.lines().get(i).right() != null)
                                <tr class="line-${fileDiff.lines().get(i).status().name().toLowerCase()}">
                                    <td class="line-number text-xs border-r border-gray-100">${i + 1}</td>
                                    <td class="px-2 py-0"><pre><code class="language-java">${fileDiff.lines().get(i).right()}</code></pre></td>
                                </tr>
                            @else
                                <tr class="bg-gray-50">
                                    <td class="line-number text-xs border-r border-gray-100"></td>
                                    <td class="px-2 py-0"></td>
                                </tr>
                            @endif
                        @endfor
                    </table>
                </div>
            </div>
        </main>
    </div>
    <script>
        // Adjust language based on file extension
        document.addEventListener('DOMContentLoaded', (event) => {
            const fileName = "${fileName}";
            let lang = "java";
            if (fileName.endsWith(".xml")) lang = "xml";
            if (fileName.endsWith(".json")) lang = "json";
            if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) lang = "yaml";
            if (fileName.endsWith(".properties")) lang = "properties";
            
            document.querySelectorAll('code').forEach((block) => {
                block.className = "language-" + lang;
            });
            Prism.highlightAll();
        });
    </script>
</body>
</html>
