import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { FieldMappingDialog } from './FieldMappingDialog';
import { parseFile, ParsedTask } from '@/utils/importUtils';

interface ImportDataProps {
  onImport: (tasks: ParsedTask[], mapping: Record<string, string>) => void;
}

export function ImportData({ onImport }: ImportDataProps) {
  const [file, setFile] = useState<File | null>(null);
  const [parsedData, setParsedData] = useState<ParsedTask[] | null>(null);
  const [headers, setHeaders] = useState<string[]>([]);
  const [isMappingDialogOpen, setIsMappingDialogOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false); // Add loading state

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setFile(e.target.files[0]);
    }
  };

  const handleParseFile = async () => {
    if (!file) {
      toast.error('Please select a file to import.');
      return;
    }

    setIsLoading(true); // Set loading to true
    try {
      const { tasks, headers: fileHeaders } = await parseFile(file);
      setParsedData(tasks);
      setHeaders(fileHeaders);
      setIsMappingDialogOpen(true);
    } catch (error) {
      toast.error('Error parsing file. Please check the file format.');
      console.error(error);
    } finally {
      setIsLoading(false); // Set loading to false
    }
  };

  const handleMappingSubmit = (mapping: Record<string, string>) => {
    if (parsedData) {
      onImport(parsedData, mapping);
    }
    setIsMappingDialogOpen(false);
    setParsedData(null);
    setHeaders([]);
    setFile(null);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Import Project Data</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid w-full max-w-sm items-center gap-1.5">
          <Label htmlFor="file">Select File (.csv, .mpp, .xer)</Label>
          <Input id="file" type="file" onChange={handleFileChange} accept=".csv,.mpp,.xer" />
        </div>
        <Button onClick={handleParseFile} className="mt-4" disabled={!file || isLoading}>
          {isLoading ? 'Processing...' : 'Import'}
        </Button>
        <FieldMappingDialog
          isOpen={isMappingDialogOpen}
          onClose={() => setIsMappingDialogOpen(false)}
          headers={headers}
          onSubmit={handleMappingSubmit}
        />
      </CardContent>
    </Card>
  );
}
